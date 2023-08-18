//tried to immitate bayeian in java


        //package com.arcore.AI_ResourceControl;
//
//import fr.inria.optimization.cmaes.CMAEvolutionStrategy;
//import fr.inria.optimization.cmaes.fitness.IObjectiveFunction;
//
//public class BayesianOptimizationExample {
//
//    // Define the reward function
//    public static class RewardFunction implements IObjectiveFunction {
//        public double valueOf(double[] x) {
//            // Calculate the reward based on the input combination x
//            // Replace this with your actual reward calculation
//            double x1 = x[0];
//            double x2 = x[1];
//            double x3 = x[2];
//            double x4 = x[3];
//
//            double reward = (x1 + x2) * x3;
//            // reward = -((x1 - 0.5) * (x1 - 0.5) + (x2 - 0.5) * (x2 - 0.5) + (x3 - 500) * (x3 - 500)); // Example reward calculation
//            return -reward; // Negative sign because CMA-ES minimizes the objective function
//        }
//
//        public boolean isFeasible(double[] x) {
//            // Implement feasibility check if required (optional).
//            // For now, assume all solutions are feasible by returning true.
//            return true;
//        }
//    }
//
//    public static void main(String[] args) {
//        int dimension = 4; // Number of dimensions in the input space
//
//        // Set the bounds for each dimension
//        double[] lowerBound = new double[]{0, 0, 0, 43};
//        double[] upperBound = new double[]{1, 1, 1, 435};
//
//        // Initialize CMA-ES with the reward function
//        CMAEvolutionStrategy cma = new CMAEvolutionStrategy();
//        cma.setDimension(dimension);
//        cma.setInitialX(0.5); // Set the initial guess for the optimization
//        cma.setInitialStandardDeviation(0.2); // Set the initial standard deviation for the optimization
//
//        // Set the bounds for each dimension
//        cma.setLowerBounds(lowerBound);
//        cma.setUpperBounds(upperBound);
//
//        // Set the reward function
//        RewardFunction rewardFunction = new RewardFunction();
//        cma.setObjectiveFunction(rewardFunction);
//
//        // Run the optimization
//        while (!cma.stopConditionsSatisfied()) {
//            cma.iterate();
//        }
//
//        // Get the best input combination and reward
//        double[] bestInput = cma.getBestX();
//        double bestReward = -cma.getBestFunctionValue(); // Note the negative sign
//
//        // Print the results
//        System.out.println("Best input combination:");
//        for (int i = 0; i < dimension; i++) {
//            System.out.println("x" + (i + 1) + ": " + bestInput[i]);
//        }
//        System.out.println("Best reward: " + bestReward);
//    }
//}
