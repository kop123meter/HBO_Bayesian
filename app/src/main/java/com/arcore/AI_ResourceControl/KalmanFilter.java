package com.arcore.AI_ResourceControl;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.sceneform.math.Vector3;
import com.google.common.base.Verify;

public class KalmanFilter implements SensorEventListener{
    private String TAG = "kalman filter";
    private float[] state = new float[9];
    private float[][] P = new float[9][9];
    private float[][] F = new float[9][9];
    private float[][] H = new float[9][9];
    private float[][] Q = new float[9][9];
    private float[][] R = new float[9][9];
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;

    private double actualTris;

    private MainActivity context;

    private double dt = 0.016;

    public KalmanFilter(MainActivity context){
        initialize();
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        startSensorUpdates();

    }

    public void startSensorUpdates() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stopSensorUpdates(){
        sensorManager.unregisterListener(this);
    }

    public void initialize(){
        for (int i = 0; i < 9; i++) {
            F[i][i] = 1;
            P[i][i] = 1;
            Q[i][i] = 0.1f;
            state[i] = 0;
        }
        for (int i = 0; i < 6; i++) {
            R[i][i] = 0.5f;
        }

        H[0][3] = 1; // vx
        H[1][4] = 1; // vy
        H[2][5] = 1; // vz
        H[3][6] = 1; // θx
        H[4][7] = 1; // θy
        H[5][8] = 1; // θz
    }

    public void predict(float[] acceleration, float[] angularVelocity) {
       // Flute sensor data

        // 更新速度
        state[3] += (float) (acceleration[0] * dt);
        state[4] += (float) (acceleration[1] * dt);
        state[5] += (float) (acceleration[2] * dt);

        // 更新位置
        state[0] += (float) (state[3] * dt);
        state[1] += (float) (state[4] * dt);
        state[2] += (float) (state[5] * dt);

        // 更新旋转角度（角速度积分）
        state[6] += (float) (angularVelocity[0] * dt); // θx
        state[7] += (float) (angularVelocity[1] * dt); // θy
        state[8] += (float) (angularVelocity[2] * dt); // θz

        P = matrixAdd(matrixMultiply(F, matrixMultiply(P, transpose(F))), Q);
    }

    public void update(float[] measurement) {
        float[][] Ht = transpose(H);
        float[][] S = matrixAdd(matrixMultiply(H, matrixMultiply(P, Ht)), R);
        float[][] K = matrixMultiply(P, matrixMultiply(Ht, inverse(S))); // Kalman

        float[] predictedTris = matrixMultiply(H, state);
        float[] y = matrixSubtract(measurement, predictedTris); // Measure residence

        // Get actual tris
        y[0] += (float) (actualTris - predictedTris[0]);

        state = matrixAdd(state, matrixMultiply(K, y));                   // update status

        float[][] I = identityMatrix(9);
        P = matrixMultiply(matrixSubtract(I, matrixMultiply(K, H)), P);   // update matrix P
    }

    public float[] getPredictedViewMatrix() {
        float[] viewMatrix = new float[16];
        Matrix.setIdentityM(viewMatrix, 0);

        Matrix.rotateM(viewMatrix, 0, (float) Math.toDegrees(state[8]), 0, 0, 1);
        Matrix.rotateM(viewMatrix, 0, (float) Math.toDegrees(state[7]), 0, 1, 0);
        Matrix.rotateM(viewMatrix, 0, (float) Math.toDegrees(state[6]), 1, 0, 0);

        Matrix.translateM(viewMatrix, 0, -state[0], -state[1], -state[2]);

        return viewMatrix;
    }

    private double[] matrixAdd(double[] a, double[] b) {
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] + b[i];
        }
        return result;
    }

    private double[] matrixSubtract(double[] a, double[] b) {
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] - b[i];
        }
        return result;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] acceleration = new float[3];
        float[] angularVelocity = new float[3];

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            System.arraycopy(event.values, 0, acceleration, 0, 3);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            System.arraycopy(event.values, 0, angularVelocity, 0, 3);
        }

        predict(acceleration, angularVelocity);

        float[] measurement = new float[]{acceleration[0], acceleration[1], acceleration[2], angularVelocity[0], angularVelocity[1], angularVelocity[2]};
        update(measurement);

        float[] predictedViewMatrix = getPredictedViewMatrix();
        Log.d(TAG, "Predicted Position: x = " + state[0] + ", y = " + state[1] + ", z = " + state[2]);
        Log.d(TAG, "Direction (theta): " + state[6] + ", " + state[7] + ", " + state[8]);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    private float[][] matrixAdd(float[][] A, float[][] B) {
        int rows = A.length, cols = A[0].length;
        float[][] result = new float[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[i][j] = A[i][j] + B[i][j];
            }
        }
        return result;
    }

    private float[] matrixAdd(float[] A, float[] B) {
        float[] result = new float[A.length];
        for (int i = 0; i < A.length; i++) {
            result[i] = A[i] + B[i];
        }
        return result;
    }
    private float[][] matrixSubtract(float[][] A, float[][] B) {
        int rows = A.length, cols = A[0].length;
        float[][] result = new float[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[i][j] = A[i][j] - B[i][j];
            }
        }
        return result;
    }

    private float[] matrixSubtract(float[] A, float[] B) {
        float[] result = new float[A.length];
        for (int i = 0; i < A.length; i++) {
            result[i] = A[i] - B[i];
        }
        return result;
    }

    private float[][] matrixMultiply(float[][] A, float[][] B) {
        int rows = A.length, cols = B[0].length, common = B.length;
        float[][] result = new float[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                for (int k = 0; k < common; k++) {
                    result[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return result;
    }

    private float[] matrixMultiply(float[][] A, float[] B) {
        float[] result = new float[A.length];
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < B.length; j++) {
                result[i] += A[i][j] * B[j];
            }
        }
        return result;
    }

    private float[][] transpose(float[][] A) {
        int rows = A.length, cols = A[0].length;
        float[][] result = new float[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[j][i] = A[i][j];
            }
        }
        return result;
    }

    private float[][] inverse(float[][] A) {
        int n = A.length;
        float[][] I = identityMatrix(n);
        float[][] augmented = new float[n][2 * n];

        // Augment the matrix with the identity matrix
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                augmented[i][j] = A[i][j];
                augmented[i][j + n] = I[i][j];
            }
        }

        // Gauss-Jordan elimination
        for (int i = 0; i < n; i++) {
            float diagElement = augmented[i][i];
            if (Math.abs(diagElement) < 1e-6) {
                Log.e(TAG, "Matrix inversion failed: singular matrix detected.");
                return identityMatrix(n);
            }
            for (int j = 0; j < 2 * n; j++) {
                augmented[i][j] /= diagElement;
            }
            for (int k = 0; k < n; k++) {
                if (k != i) {
                    float factor = augmented[k][i];
                    for (int j = 0; j < 2 * n; j++) {
                        augmented[k][j] -= factor * augmented[i][j];
                    }
                }
            }
        }

        // Extract the inverse matrix
        float[][] inverse = new float[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                inverse[i][j] = augmented[i][j + n];
            }
        }
        return inverse;
    }

    private float[][] identityMatrix(int size) {
        float[][] I = new float[size][size];
        for (int i = 0; i < size; i++) {
            I[i][i] = 1.0f;
        }
        return I;
    }

    public void setActualTris(double tris){
        actualTris = tris;
    }



}