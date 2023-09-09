#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
This is to test the delegate translation from the continious values to dicrete
I have implemented it to my Java app
Created on Fri Sep  8 17:02:49 2023

@author: niloofar
"""

import numpy as np


x1= 0.44
x2= 0.40
x3= 0.14
'''
x1=0.6
x2=0.19
x3=0.21
'''
# Define the percentage vector and the target sum N
percentage_vector = np.array([x1,x2,x3])
N = 3  # Replace with your desired target sum

# Scale the percentages to integers based on their proportion
scaled_values = (percentage_vector  * N).astype(int)

# Adjust the values to ensure they sum up to N
remainder = N - np.sum(scaled_values)

# Distribute the remainder evenly among the values



if remainder > 0:
    
    sorted_indices = np.argsort(percentage_vector)[::-1]
    sorted_percentages = percentage_vector[sorted_indices]


# Allocate integers to the devices with the highest percentages
    for i in range(N):
       scaled_values[sorted_indices[i]] += 1

       remainder-=1
       if remainder<= 0:
           break

# Now, scaled_values contains the mapped integers
print("Mapped integers:", scaled_values)

'''

'''