package sintef.android.controller.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import java.util.Date;
import java.util.HashMap;

import de.greenrobot.event.EventBus;
import sintef.android.controller.EventTypes;
import sintef.android.controller.common.ClientPaths;
import sintef.android.controller.common.Constants;
import sintef.android.controller.sensor.data.LinearAccelerationData;
import sintef.android.controller.sensor.data.MagneticFieldData;
import sintef.android.controller.sensor.data.RotationVectorData;
import sintef.android.controller.sensor.data.SensorDataObject;

/**
 * Created by samyboy89 on 03/02/15.
 */
public class SensorManager implements SensorEventListener {

    private EventBus mEventBus;

    private android.hardware.SensorManager mSensorManager;
    private HashMap<Integer, SensorSession> mSensorGroup = new HashMap<>();
    private RemoteSensorManager mRemoteSensorManager;

    private static SensorManager instance;

    public static synchronized SensorManager getInstance(Context context) {
        if (instance == null) {
            instance = new SensorManager(context.getApplicationContext());
        }

        return instance;
    }

    private SensorManager(Context context) {
        mEventBus = EventBus.getDefault();
        mEventBus.registerSticky(this);

        mSensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        mRemoteSensorManager = RemoteSensorManager.getInstance(context);

        addSensorToSystem("phone:magnetic_field", Sensor.TYPE_MAGNETIC_FIELD, SensorDevice.PHONE, SensorLocation.RIGHT_PANT_POCKET);
        addSensorToSystem("phone:linear_acceleration", Sensor.TYPE_LINEAR_ACCELERATION, SensorDevice.PHONE, SensorLocation.RIGHT_PANT_POCKET);
        addSensorToSystem("phone:rotation_vector", Sensor.TYPE_ROTATION_VECTOR, SensorDevice.PHONE, SensorLocation.RIGHT_PANT_POCKET);

    }

    private void addSensorToSystem(String id, int type, SensorDevice device, SensorLocation location) {
        SensorSession sensorSession = new SensorSession(id, type, device, location);
        mSensorGroup.put(type, sensorSession);
    }

    public void onEvent(EventTypes eventType) {
        switch (eventType) {
            case ONRESUME:
                mRemoteSensorManager.setMode(ClientPaths.MODE_DEFAULT);
            case RESET_SENSOR_LISTENERS:
                mSensorManager.unregisterListener(this);

                for (int type : mSensorGroup.keySet()) {
                    mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(type), Constants.SENSOR_PULL_FREQ);
                }
                break;
            case ONDESTROY:
                mSensorManager.unregisterListener(this);
                break;
        }
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(final SensorEvent event) {
        SensorDataObject sensorDataObject = null;
        switch (event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                sensorDataObject = new MagneticFieldData(event.values.clone());
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                sensorDataObject = new LinearAccelerationData(event.values.clone());
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                sensorDataObject = new RotationVectorData(event.values.clone());
                break;
        }

        if (sensorDataObject != null)  {
            // sensor event timestamps are time since system boot
            long timestamp = (new Date()).getTime() + (event.timestamp - System.nanoTime()) / 1000000L;
            mEventBus.post(new SensorData(mSensorGroup.get(event.sensor.getType()), sensorDataObject, timestamp));
        }
    }
}
