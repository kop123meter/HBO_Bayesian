package com.arcore.AI_ResourceControl;



public class LinearRegression {
    public double intercept, slope, rmse;
    //private final double r2;
    //private final double svar0, svar1;

    /**
     * Performs a linear regression on the data points {@code (y[i], x[i])}.
     *
     * @param  x the values of the predictor variable
     * @param  y the corresponding values of the response variable
     * @throws IllegalArgumentException if the lengths of the two arrays are not equal
     */
    public   LinearRegression(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("array lengths are not equal");
        }
        int n = x.length;

        // first pass
        double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;
        for (int i = 0; i < n; i++) {
            sumx  += x[i];
            sumx2 += x[i]*x[i];
            sumy  += y[i];
        }
        double xbar = sumx / n;
        double ybar = sumy / n;

        // second pass: compute summary statistics
        double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
        for (int i = 0; i < n; i++) {
            xxbar += (x[i] - xbar) * (x[i] - xbar);
            yybar += (y[i] - ybar) * (y[i] - ybar);
            xybar += (x[i] - xbar) * (y[i] - ybar);
        }
        slope  = xybar / xxbar;
        intercept = ybar - slope * xbar;

        // more statistical analysis
        double sse = 0.0;      //  sum of square error
      //  double ssr = 0.0;      // regression sum of squares
        for (int i = 0; i < n; i++) {
            double fit = slope*x[i] + intercept;
            sse += (fit - y[i]) * (fit - y[i]);// sum of square error

        }

        double mse= sse/n;
        rmse= Math.sqrt(mse);

        double [] slopeInc= new double[]{slope, intercept};


    }

    public void setRmse(double rmse) {
        this.rmse = rmse;
    }

    public double getRmse() {
        return rmse;
    }

    /**
     * Returns the <em>y</em>-intercept α of the best of the best-fit line <em>y</em> = α + β <em>x</em>.
     *
     * @return the <em>y</em>-intercept α of the best-fit line <em>y = α + β x</em>
     */
    public double intercept() {
        return intercept;
    }

    /**
     * Returns the slope β of the best of the best-fit line <em>y</em> = α + β <em>x</em>.
     *
     * @return the slope β of the best-fit line <em>y</em> = α + β <em>x</em>
     */
    public double slope() {
        return slope;
    }

    /**
     * Returns the coefficient of determination <em>R</em><sup>2</sup>.
     *
     * @return the coefficient of determination <em>R</em><sup>2</sup>,
     *         which is a real number between 0 and 1
     */
//    public double R2() {
//        return r2;
//    }
//
//    /**
//     * Returns the standard error of the estimate for the intercept.
//     *
//     * @return the standard error of the estimate for the intercept
//     */
//    public double interceptStdErr() {
//        return Math.sqrt(svar0);
//    }
//
//    /**
//     * Returns the standard error of the estimate for the slope.
//     *
//     * @return the standard error of the estimate for the slope
//     */
//    public double slopeStdErr() {
//        return Math.sqrt(svar1);
//    }

    /**
     * Returns the expected response {@code y} given the value of the predictor
     * variable {@code x}.
     *
     * @param  x the value of the predictor variable
     * @return the expected response {@code y} given the value of the predictor
     *         variable {@code x}
     */
    public double predict(double x) {
        return slope*x + intercept;
    }

    /**
     * Returns a string representation of the simple linear regression model.
     *
     * @return a string representation of the simple linear regression model,
     *         including the best-fit line and the coefficient of determination
     *         <em>R</em><sup>2</sup>
     */
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(String.format("%.2f n + %.2f", slope(), intercept()));
        s.append("  (RMSE = " + String.format("%.3f", getRmse()) + ")");
        return s.toString();
    }

}