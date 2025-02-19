package com.arcore.AI_ResourceControl;

import org.apache.commons.math3.linear.*;

public class KalmanFilter{

    private RealVector state;
    private RealMatrix P;
    private RealMatrix Q;
    private RealMatrix R;
    private RealMatrix H;

    public KalmanFilter() {
//        state = new ArrayRealVector(new double[]{0, 0, 0, 1, 0, 0, 0, 0, 0, 0}); // [px, py, pz, qw, qx, qy, qz, vx, vy, vz]
        P = MatrixUtils.createRealIdentityMatrix(7).scalarMultiply(0.1);
        Q = MatrixUtils.createRealIdentityMatrix(7).scalarMultiply(0.01);
        R = MatrixUtils.createRealIdentityMatrix(7).scalarMultiply(0.1);
        H = new Array2DRowRealMatrix(7, 7);
        H.setSubMatrix(new double[][]{
                {1, 0, 0, 0, 0, 0, 0},  // x
                {0, 1, 0, 0, 0, 0, 0},  // y
                {0, 0, 1, 0, 0, 0, 0},  // z
                {0, 0, 0, 1, 0, 0, 0},  // qw
                {0, 0, 0, 0, 1, 0, 0},  // qx
                {0, 0, 0, 0, 0, 1, 0},   // qy
                {0, 0, 0, 0, 0, 0, 1},   // qz
        }, 0, 0);
    }

    public void setInitialStates(double[] mesurements){
        state = new ArrayRealVector(mesurements);
    }


    public void predict(double dt, double[] angularVelocity, double[] linearVelocity) {
        double[] pos = state.getSubVector(0, 3).toArray();
        double[] quat = state.getSubVector(3, 4).toArray();



        double[] omega = new double[]{
                angularVelocity[0] * 0.5 * dt,
                angularVelocity[1] * 0.5 * dt,
                angularVelocity[2] * 0.5 * dt
        };
        double qw = quat[0] - omega[0] * quat[1] - omega[1] * quat[2] - omega[2] * quat[3];
        double qx = quat[1] + omega[0] * quat[0] + omega[2] * quat[2] - omega[1] * quat[3];
        double qy = quat[2] + omega[1] * quat[0] - omega[2] * quat[1] + omega[0] * quat[3];
        double qz = quat[3] + omega[2] * quat[0] + omega[1] * quat[1] - omega[0] * quat[2];
        quat = new double[]{qw, qx, qy, qz};


        // Transfer to world speed
        double[][] rotation_matrix = quaternionToRotationMatrix(quat);
        double[] vel = applyRotation(rotation_matrix,linearVelocity);

        for (int i = 0; i < 3; i++) {
            pos[i] += vel[i] * dt;
        }



//        double norm = Math.sqrt(qw * qw + qx * qx + qy * qy + qz * qz);
//        for (int i = 0; i < 4; i++) {
//            quat[i] /= norm;
//        }

        // update states
        state.setSubVector(0, new ArrayRealVector(pos));
        state.setSubVector(3, new ArrayRealVector(quat));


        // update covariance  matrix
        RealMatrix F = MatrixUtils.createRealIdentityMatrix(7);

//        RealMatrix dPos_dQuat = computePositionJacobian(state.getSubVector(3, 4).toArray(), dt);
//        F.setSubMatrix(dPos_dQuat.getData(), 0, 3);
//        RealMatrix dQuat_dOmega = computeQuaternionJacobian(state.getSubVector(3, 4).toArray(), angularVelocity, dt);
//        F.setSubMatrix(dQuat_dOmega.getData(), 3, 3);


        P = F.multiply(P).multiply(F.transpose()).add(Q);
    }


    public void update(double[] measurement) {
        RealVector z = new ArrayRealVector(measurement);  // Measured value
        RealVector y = z.subtract(H.operate(state));  // Compute covariance

        RealMatrix S = H.multiply(P).multiply(H.transpose()).add(R);

        double innovationNorm = S.getNorm();
        double threshold = 5;

        if (innovationNorm > threshold) {
            Q = Q.scalarMultiply(1.2);
            R = R.scalarMultiply(0.9);
        } else {
            Q = Q.scalarMultiply(0.9);
            R = R.scalarMultiply(1.1);
        }
        RealMatrix K = P.multiply(H.transpose()).multiply(new LUDecomposition(S).getSolver().getInverse());

        state = state.add(K.operate(y));  // update state
        P = MatrixUtils.createRealIdentityMatrix(7).subtract(K.multiply(H)).multiply(P);
    }
    public double[] getPosition(){
        return state.getSubVector(0, 3).toArray();
    }


    public double[] getViewMatrix() {
        double[] pos = state.getSubVector(0, 3).toArray();
        double[] quat = state.getSubVector(3, 4).toArray();

        double[][] viewMatrix = new double[4][4];
        double qw = quat[0], qx = quat[1], qy = quat[2], qz = quat[3];

        viewMatrix[0][0] = 1 - 2 * (qy * qy + qz * qz);
        viewMatrix[0][1] = 2 * (qx * qy - qw * qz);
        viewMatrix[0][2] = 2 * (qx * qz + qw * qy);
        viewMatrix[0][3] = -pos[0];

        viewMatrix[1][0] = 2 * (qx * qy + qw * qz);
        viewMatrix[1][1] = 1 - 2 * (qx * qx + qz * qz);
        viewMatrix[1][2] = 2 * (qy * qz - qw * qx);
        viewMatrix[1][3] = -pos[1];

        viewMatrix[2][0] = 2 * (qx * qz - qw * qy);
        viewMatrix[2][1] = 2 * (qy * qz + qw * qx);
        viewMatrix[2][2] = 1 - 2 * (qx * qx + qy * qy);
        viewMatrix[2][3] = -pos[2];

        viewMatrix[3][3] = 1;

        return new double[]{
                viewMatrix[0][0], viewMatrix[0][1], viewMatrix[0][2], viewMatrix[0][3],
                viewMatrix[1][0], viewMatrix[1][1], viewMatrix[1][2], viewMatrix[1][3],
                viewMatrix[2][0], viewMatrix[2][1], viewMatrix[2][2], viewMatrix[2][3],
                0, 0, 0, 1
        };
    }

    public static double[][] quaternionToRotationMatrix(double[] q) {
        double qw = q[0], qx = q[1], qy = q[2], qz = q[3];

        double[][] R = new double[3][3];

        R[0][0] = 1 - 2 * (qy * qy + qz * qz);
        R[0][1] = 2 * (qx * qy - qw * qz);
        R[0][2] = 2 * (qx * qz + qw * qy);

        R[1][0] = 2 * (qx * qy + qw * qz);
        R[1][1] = 1 - 2 * (qx * qx + qz * qz);
        R[1][2] = 2 * (qy * qz - qw * qx);

        R[2][0] = 2 * (qx * qz - qw * qy);
        R[2][1] = 2 * (qy * qz + qw * qx);
        R[2][2] = 1 - 2 * (qx * qx + qy * qy);

        return R;
    }

    public static double[] applyRotation(double[][] R, double[] localVelocity) {
        double[] worldVelocity = new double[3];

        for (int i = 0; i < 3; i++) {
            worldVelocity[i] = R[i][0] * localVelocity[0] +
                    R[i][1] * localVelocity[1] +
                    R[i][2] * localVelocity[2];
        }

        return worldVelocity;
    }


    public RealMatrix computeQuaternionJacobian(double[] quat, double[] angularVelocity, double dt) {
        double wx = angularVelocity[0];
        double wy = angularVelocity[1];
        double wz = angularVelocity[2];

        // 构建 Omega(ω) 矩阵
        double[][] omega = {
                {0, -wx, -wy, -wz},
                {wx, 0, wz, -wy},
                {wy, -wz, 0, wx},
                {wz, wy, -wx, 0}
        };

        RealMatrix omegaMatrix = new Array2DRowRealMatrix(omega);


        RealMatrix identity = MatrixUtils.createRealIdentityMatrix(4);
        RealMatrix jacobian = identity.add(omegaMatrix.scalarMultiply(0.5 * dt));

        return jacobian;
    }



    public RealMatrix computePositionJacobian(double[] quat, double dt) {
        double qw = quat[0], qx = quat[1], qy = quat[2], qz = quat[3];

        // Define the partial derivatives of the rotation matrix w.r.t quaternion elements
        double[][] dR_dqw = {
                { 0, -2*qz,  2*qy},
                { 2*qz,  0, -2*qx},
                {-2*qy,  2*qx,  0}
        };
        double[][] dR_dqx = {
                { 0,  2*qy,  2*qz},
                { 2*qy, -4*qx, -2*qw},
                { 2*qz,  2*qw, -4*qx}
        };
        double[][] dR_dqy = {
                {-4*qy,  2*qx,  2*qw},
                { 2*qx,  0,  2*qz},
                {-2*qw,  2*qz, -4*qy}
        };
        double[][] dR_dqz = {
                {-4*qz, -2*qw,  2*qx},
                { 2*qw, -4*qz,  2*qy},
                { 2*qx,  2*qy,  0}
        };

        // Convert to RealMatrix
        RealMatrix dR_dqw_Matrix = new Array2DRowRealMatrix(dR_dqw);
        RealMatrix dR_dqx_Matrix = new Array2DRowRealMatrix(dR_dqx);
        RealMatrix dR_dqy_Matrix = new Array2DRowRealMatrix(dR_dqy);
        RealMatrix dR_dqz_Matrix = new Array2DRowRealMatrix(dR_dqz);

        // Local velocity in the body frame (assuming it is known)
        double[] velocity = {1, 1, 1}; // Replace with actual velocity
        RealMatrix vMatrix = new Array2DRowRealMatrix(velocity);

        // Compute Jacobian matrix: J = [dR/dqw * v, dR/dqx * v, dR/dqy * v, dR/dqz * v]
        RealMatrix J = new Array2DRowRealMatrix(3, 4);
        J.setColumnVector(0, dR_dqw_Matrix.multiply(vMatrix).getColumnVector(0).mapMultiply(dt));
        J.setColumnVector(1, dR_dqx_Matrix.multiply(vMatrix).getColumnVector(0).mapMultiply(dt));
        J.setColumnVector(2, dR_dqy_Matrix.multiply(vMatrix).getColumnVector(0).mapMultiply(dt));
        J.setColumnVector(3, dR_dqz_Matrix.multiply(vMatrix).getColumnVector(0).mapMultiply(dt));

        return J;
    }






}