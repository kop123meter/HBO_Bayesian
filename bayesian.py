#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed Jul 19 16:59:33 2023

@author: niloofar
"""

#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Jul 17 10:24:10 2023

@author: niloofar
"""
'''





'''


#from sklearn import datasets, linear_model
#from sklearn.metrics import mean_squared_error, r2_score
from math import isnan
from statistics import mean
#from sklearn.preprocessing import PolynomialFeatures
#from sklearn.linear_model import LinearRegression
#import pandas as pd
import numpy as np
import random
#import matplotlib.pyplot as plt
import csv
import statistics
import math 
#from sklearn.datasets import load_boston
#from sklearn.linear_model import LinearRegression
#from sklearn.metrics import mean_squared_error, r2_score
#from matplotlib import pyplot as plt
#from matplotlib import pyplot as plt2
#from matplotlib import pyplot as plt3
import os.path
from os import path
#from matplotlib import rcParams
import datetime 
from datetime import datetime
#from scipy.stats import linregress
#import matplotlib as mpl
import GPyOpt
import GPy
#import numpy as npv
#import numpy as np
#import pandas as pd
#from sklearn import datasets, linear_model
#from sklearn.metrics import mean_squared_error, r2_score
from math import isnan
from statistics import mean
#from sklearn.preprocessing import PolynomialFeatures
#from sklearn.linear_model import LinearRegression
#import pandas as pd
#import numpy as np
import random
#import matplotlib.pyplot as plt
import csv
import statistics
import math 
'''
from sklearn.datasets import load_boston
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_squared_error, r2_score
from matplotlib import pyplot as plt
from matplotlib import pyplot as plt2
from matplotlib import pyplot as plt3
'''
import os.path
from os import path
#from matplotlib import rcParams
import datetime 
from datetime import datetime
from scipy.stats import linregress
#import matplotlib as mpl
#from matplotlib.pyplot import figure
import GPyOpt
from GPyOpt.methods import BayesianOptimization
#numpy
#import numpy as np
#from numpy.random import multivariate_normal #For later example
#Plotting tools
from mpl_toolkits.mplot3d import Axes3D
#mport matplotlib.pyplot as plt
#from matplotlib import cm
#from matplotlib.ticker import LinearLocator, FormatStrFormatter
#import numpy as np
#from numpy.random import multivariate_normal

import itertools
from operator import itemgetter
from operator import itemgetter
#import numpy as np
import GPyOpt
import random
from itertools import product
from bayes_opt import BayesianOptimization
from bayes_opt.util import UtilityFunction
# Define the reward function





def reward_function(x):
    # Calculate the reward based on the input combination x
    # Replace this with your actual reward calculation
    x1, x2, x3,x4 = x[:, 0], x[:, 1], x[:, 2], x[:, 3],
    reward= (x1+x2 )*x3
    #reward= get_result_element(input_data,  [x1,x2,x3,x4])
    return (reward*-1)
    #reward = -((x1 - 0.5)**2 + (x2 - 0.5)**2 + (x3 - 500)**2)  # Example reward calculation
    #return reward.reshape(-1, 1)

# Define the bounds of the input variables
bounds = [{'name': 'x1', 'type': 'discrete', 'domain': (0, 0.3, 0.7, 1)},
          {'name': 'x2', 'type': 'discrete', 'domain': (0, 0.3, 0.7, 1)},
          {'name': 'x3', 'type': 'discrete', 'domain': (0, 0.3, 0.7, 1)},
         {'name': 'x4', 'type': 'discrete', 'domain': (435, 217, 43)},]
          #{'name': 'x4', 'type': 'continuous', 'domain': (100, 1000)}]

'''
###########test
'''

max_iter = 5 # Number of optimization iterations
(max_iter)
max_time  = None 
tolerance = 1e-8 
# --- CHOOSE the type of acquisition

space = [{'name': 'var_1', 'type': 'discrete', 'domain': (0, 0.3, 0.7, 1)},
          {'name': 'var_2', 'type': 'discrete', 'domain': (0, 0.3, 0.7, 1)},
         {'name': 'var_3', 'type': 'discrete', 'domain': (0, 0.3, 0.7, 1)},
        {'name': 'var_4', 'type': 'discrete', 'domain':  (435, 217, 43)}
          ]

#feasible_region = GPyOpt.Design_space(space = space, constraints= [{'name': 'constr_1', 'constraint': 'np.abs(x[:, 0] + x[:, 1] + x[:, 2]- 1) '}])

# --- CHOOSE the model type
#model = GPyOpt.models.GPModel(exact_feval=True,optimize_restarts=10,verbose=False)

# --- CHOOSE the acquisition optimizer
#aquisition_optimizer = GPyOpt.optimization.AcquisitionOptimizer(feasible_region)

# --- CHOOSE the type of acquisition
#acquisition = GPyOpt.acquisitions.AcquisitionEI(model, feasible_region, optimizer=aquisition_optimizer)

'''
#########test
'''

# Create the optimization problem
#problem = GPyOpt.methods.BayesianOptimization(f=reward_function, domain=bounds)
problem = GPyOpt.methods.BayesianOptimization(reward_function, domain=space , constraints= [{'name': 'constr_1', 'constraint': 'np.abs(x[:, 0] + x[:, 1] +x[:, 2] - 1) '}])
# distance between two consecutive observations  
# Run the optimization                                                  
problem.run_optimi
zation(max_iter = max_iter)
                         #, max_time = max_time, eps = tolerance, verbosity=False) 
#problem.plot_acquisition()
#problem.plot_convergence()
best_input = problem.x_opt
best_reward = problem.fx_opt

#upper confidence bounds by default has a good  convergence rate


print("Best input combination: for iteration #" +str(max_iter), best_input)
print("Best reward:", best_reward)


with open ("result.txt", "w")  as out:
    out.write("Best reward:"+str( best_reward))

'''
max_iter  = 20
problem.run_optimization(max_iter = max_iter, max_time = max_time, eps = tolerance, verbosity=False) 


# Get the best input combination and reward
best_input = problem.x_opt
best_reward = problem.fx_opt

print("Value of input that minimises the objective: for iteration #" +str(max_iter), best_input)
print("Minimum value of the objective:", best_reward)

#fig, ax = plt.subplots()
#problem.plot_acquisition()
#problem.plot_convergence()


'''

