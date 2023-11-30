#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sat Sep 30 15:49:06 2023

@author: niloofar
"""

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

num_tasks=3
x=[0.4,0.5,0.1]



def translate_delegate_usage(x):
    percentage_vector =x
    #np.array([x1,x2,x3])
    N = num_tasks  # Replace with your desired target sum

    # Scale the percentages to integers based on their proportion
    scaled_values = [int(value * N) for value in percentage_vector]

    # Adjust the values to ensure they sum up to N
    remainder = N - np.sum(scaled_values)

    # Distribute the remainder evenly among the values


    if remainder > 0:
        
        #sorted_indices = np.argsort(percentage_vector)[::-1]
        #sorted_percentages = percentage_vector[sorted_indices]
        sorted_indices = list(np.argsort(percentage_vector)[::-1])
        sorted_percentages = [percentage_vector[i] for i in sorted_indices]

    # Allocate integers to the devices with the highest percentages
        for i in range(N):
           scaled_values[sorted_indices[i]] += 1

           remainder-=1
           if remainder<= 0:
               break
          
    float_output = [float(x) for x in scaled_values]        
    return float_output


translate_delegate_usage(x)