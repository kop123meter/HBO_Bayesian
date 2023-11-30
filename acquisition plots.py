#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sun Sep 24 20:03:32 2023

@author: niloofar
"""

from math import isnan
from statistics import mean
from sklearn.preprocessing import PolynomialFeatures
from sklearn.linear_model import LinearRegression
import pandas as pd
import numpy as np
import random
import matplotlib.pyplot as plt
import csv
import statistics
import math 
from sklearn.datasets import load_boston
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_squared_error, r2_score
from matplotlib import pyplot as plt
from matplotlib import pyplot as plt2
from matplotlib import pyplot as plt3
import os.path
from os import path
from matplotlib import rcParams
import datetime 
from datetime import datetime
from scipy.stats import linregress
import matplotlib as mpl

#mpl.rcParams['axes.formatter.useoffset'] = False

from matplotlib.pyplot import figure


plt.rcParams['font.size'] = '17'
plt.rcParams['lines.markersize'] = 10
plt.rcParams["font.family"] = "Times New Roman"
plt.rcParams["axes.labelweight"] = "bold"
plt.rcParams["figure.autolayout"] = True




root="sep-AfterNormalization/"
test=root+"FINAL conf new-sc9"
series="18:16"


bayesNoTris_data= pd.read_csv(str(test)+"/bayesNoTris.csv" )#bayesian3 without Triangle change
bayes_data= pd.read_csv(str(test)+"/Bayesian_dataCollection"+str(series)+".csv")  # data of app for bayesian
python_bayes_data= pd.read_csv(str(test)+"/Bayesian OUTPUT"+str(series)+".csv")  
baseDat12= pd.read_csv(str(test)+"/Static_dataCollection"+str(series)+".csv") 

cpubaseDat12= pd.read_csv(str(test)+"/CPU_Mem_Base12"+".csv")  
cpubayes_data= pd.read_csv(str(test)+"/CPU_Mem_"+str(series)+".csv") 
cpuNotDat= pd.read_csv(str(test)+"/CPU_Mem_noTris"+".csv")  
cpuAllNdata= pd.read_csv(str(test)+"/CPU_Mem_AllNNAPI"+".csv") 

avgQBYS=list(bayes_data['avgQ'].values.reshape(-1,1)[:,0]) #

## data of bayesian fetched
bYReward=list(python_bayes_data['reward'].values.reshape(-1,1)[:-2,0]) # 
ttris=list(python_bayes_data['ttris'].values.reshape(-1,1)[:-2,0]) # 
cu=list(python_bayes_data['cu'].values.reshape(-1,1)[:-2,0]) # cpu usage
gu=list(python_bayes_data['gu'].values.reshape(-1,1)[:-2,0]) # Gpu usage
nu=list(python_bayes_data['nu'].values.reshape(-1,1)[:-2,0]) # NNAPI usage
tris_ratio=list(python_bayes_data['tris'].values.reshape(-1,1)[:-2,0]) # 
ct=list(python_bayes_data['ct'].values.reshape(-1,1)[:-2,0]) # traslated cpu usage
gt=list(python_bayes_data['gt'].values.reshape(-1,1)[:-2,0]) # 
nt=list(python_bayes_data['nt'].values.reshape(-1,1)[:-2,0]) # 
ttris=list(python_bayes_data['ttris'].values.reshape(-1,1)[:-2,0]) # 




list1 = [1.0, 2.0, 3.0, 4.0]
list2 = [5.0, 6.0, 7.0, 8.0]
list3 = [9.0, 10.0, 11.0, 12.0]
list4 = [13.0, 14.0, 15.0, 16.0]
list5 = [17.0, 18.0, 19.0, 20.0]
list6 = [21.0, 22.0, 23.0, 24.0]

# Create a NumPy array from the lists
tested_data = np.array([list1, list2, list3, list4, list5, list6], dtype=np.float64)
minReward=[]
min_value = float('inf')
rewardList=[0.5,0.4,0.6,0.1,0.4,0.02]
for reward in rewardList:
    min_value = min(min_value, reward)  # Calculate the minimum up to the current index
    minReward.append(min_value)  # Append the minimum to the new list

print(minReward)




def test_plot_convergence(Xdata, best_Y, filename=None):
    
    #Plots to evaluate the convergence of standard Bayesian optimization algorithms
    
    plt.rcParams['font.size'] = '17'
    plt.rcParams['lines.markersize'] = 10
    plt.rcParams["font.family"] = "Times New Roman"
    plt.rcParams["axes.labelweight"] = "bold"
    plt.rcParams["figure.autolayout"] = True
    
    n = Xdata.shape[0]
    aux = (Xdata[1:n,:]-Xdata[0:n-1,:])**2
    distances = np.sqrt(aux.sum(axis=1))

    ## Distances between consecutive x's
    #plt.figure(figsize=(10,5))
    #plt.subplot(1, 2, 1)
    fig, ax = plt.subplots()
    plt.plot(list(range(n-1)), distances, '-ro')
    plt.xlabel('Iteration')
    plt.ylabel('d(x[n], x[n-1])')
    #plt.title('Distance between consecutive x\'s')
    #grid(True)
    if filename!=None:
        fig.savefig( str(filename)+"distance.pdf" , dpi=300)
        
    else:
        plt.show()

    # Estimated m(x) at the proposed sampling points
   # plt.subplot(1, 2, 2)
    fig, ax = plt.subplots()
    plt.plot(list(range(n)),best_Y,'-o')
    #plt.title('Value of the best selected sample')
    plt.xlabel('Iteration')
    plt.ylabel('Best y')
    #grid(True)

    if filename!=None:
        fig.savefig( str(filename)+".pdf" , dpi=300)
    else:
        plt.show()

test_plot_convergence(tested_data,minReward,filename="test_convergence_plot")
