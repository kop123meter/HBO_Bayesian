'''This is the bayesian without triangle count change'''


import json
import socket
from GPyOpt.methods import BayesianOptimization
import random
import numpy as np
import matplotlib.pyplot as plt

import time

# Get the current time in seconds since the epoch
import datetime


current_time= datetime.datetime.now()
x = current_time.strftime("%H:%M")
print(x)
counter=0


with open ("Bayesian OUTPUT"+str(x)+".csv", "w")  as out:
    out.write(",cu,,gu,,nu,,tris,,,ct,,gt,,nt,,ttris,,reward \n")
    
num_tasks=6

def quick_test(X):
    #x1, x2, x3, x4 = X[:, 0], X[:, 1], X[:, 2],X[:, 3]
    for i in range(0,3):
      print( "x"+str(i+1)+": "+ str(X[0][i]))
      print( "4x"+str(i+1)+": "+ str(X[0][i]*4))
      print( "ROUND"+str(i+1)+": "+ str(round(X[0][i]*4)))
    X_list = X.tolist()
   
    print( "input: "+ str(X_list))
 
    return X[0][2]


def objective( X):
    
        global counter
        counter+=1
        translatedU=[0,0,0]
        translatedU[0:3]=translate_delegate_usage(X[0][0:3])
        #translatedU[3]=(X[0][3])
    
        # Convert X to a list (assuming X is a numpy array)
        X_list =[]
        X_list.append( list(translatedU))
        
        #old version
        nontranslated_X_list = X.tolist()

        # Prepare data to send to the Java server
        data = {
           "python_client"
            : 
                #translatedU
                X_list[0]
        }

        #data2 = X_list[0]
     
        # Convert data to JSON string
        json_data = json.dumps(data)       

        # Connect to the Java server
        #host = 'localhost'  # Replace with the IP or hostname of your Java server
        #port = 12345        # Replace with the port number on which your Java server is listening
       
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client_socket:
            
            client_socket.connect(('127.0.0.1', 4444))
            #connected = client_socket.recv(1024).decode()
            #print("Recieved! "+ str(connected))
            
            
            client_socket.sendall(json_data.encode()+ b'\n') ##'\n' is a must Add a newline character very important@@@@ this is correct
           
            print(  str(counter)+": "+str(nontranslated_X_list[0])+" is sent, waiting for the reward ...")
            print(  " delegate is translated to", str(X_list[0]))
      
           
            #Receive the reward value from the Java server
            received_data = client_socket.recv(1024).decode()
            print("Recieved reward : "+ str(received_data))
         
            with open ("Bayesian OUTPUT"+str(x)+".csv", "a")  as out:
               out.write(str(nontranslated_X_list[0])+","+str(X_list[0])+","+str(received_data))
        
        #received_data=X_list[0][2]
        return (float(received_data)*-1) # this should pass the reward function from java server after calculating the mean quality and average AI inference response time


def translate_delegate_usage(x):
    percentage_vector =x
    #np.array([x1,x2,x3])
    N = num_tasks  # Replace with your desired target sum

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
          
    float_output = [float(x) for x in scaled_values]        
    return float_output


class JavaRewardBayesianOptimization(BayesianOptimization):
  
    #def __init__(self, domain , acquisition_type='EI', acquisition_optimizer_type=None, model_type='GP'):
   # normalize_Y=False,         exact_feval=False, acquisition_jitter=0.01, num_cores=None, verbosity=False, **kwargs):
        # super(JavaRewardBayesianOptimization, self).__init__(reward_function,domain, acquisition_type, acquisition_optimizer_type,
          #                                                   model_type)
                                                           #  normalize_Y, exact_feval, acquisition_jitter,
                                                            # num_cores, verbosity, **kwargs)
    
    

                             
    
    def __init__(self, domain,constraints  ):
     #super(JavaRewardBayesianOptimization, self).__init__(reward_function,domain=domain,constraints = constraints)
   
    # runs the exploreation phase for 5 times!
       super(JavaRewardBayesianOptimization, self).__init__( f=objective,domain=domain,constraints = constraints,acquisition_type ='EI')
       #super(JavaRewardBayesianOptimization, self).__init__( f=quick_test,domain=domain,constraints = constraints,acquisition_type ='EI')
                                                   
                               
   
'''
    {'name': 'var_1', 'type': 'discrete', 'domain': (0, 0.3, 0.7, 1)},
      {'name': 'var_2', 'type': 'discrete', 'domain': (0, 0.3, 0.7, 1)},
     {'name': 'var_3', 'type': 'discrete', 'domain': (0, 0.3, 0.7, 1)},
''' 
# for 5 splanes
    #{'name': 'var_4', 'type': 'continuous', 'domain':(0.28,0.62), 'dimensionality' :1},# for 4 andies
    

#problem = GPyOpt.methods.BayesianOptimization(reward_function, domain=space , constraints= [{'name': 'constr_1', 'constraint': 'np.abs(x[:, 0] + x[:, 1] +x[:, 2] - 1) '}])


#constraint2 = [{'name': 'var_4_constraint', 'constraint': var_4_constraint}]

space = [     {'name': 'var_1', 'type': 'continuous', 'domain': [0,1]},
     {'name': 'var_2', 'type': 'continuous', 'domain': [0,1]},
     {'name': 'var_3', 'type': 'continuous', 'domain': [0,1]},
   #  {'name': 'var_4', 'type': 'continuous', 'domain':(0.05,1), 'dimensionality' :1}
     ]


# Initialize the custom optimization class with the Java reward function
problem = JavaRewardBayesianOptimization(domain=space,constraints= [{'name': 'constr_1','constraint': 'x[:,0]+ x[:,1]+ x[:, 2] -1'},
                                                          {'name': 'constr_2','constraint': '-x[:,0]- x[:,1]- x[:, 2] +0.999'}
                                                          
                                                          
                                                                    ])

                                                                     #'constraint': 'np.abs(x[:, 0] + x[:, 1] +x[:, 2] - 1) '}])
#,  {'name': 'constr_2', 'constraint': 'np.round(x[:,3],3)-x[:,3]'}
 

'''problem = JavaRewardBayesianOptimizatio gos for exploring 5 different options and in the next lines we will run optimization
to exploit'''

    
print("Initial exploration is finished!")
max_iter  = 15
problem.run_optimization(max_iter = max_iter)                                      
                

best_input = problem.x_opt
best_reward = problem.fx_opt

#upper confidence bounds by default has a good  convergence rate


print("Best input combination: for iteration count #" +str(max_iter), best_input)
print("Best reward:", best_reward)
with open ("Bayesian OUTPUT"+str(x)+".csv", "a")  as out:
    out.write( str( best_input) +","+str( best_reward)+"\n")
''''This is the application of the best input to our app after the max trial'''

translatedU=[0,0,0]
translatedU[0:3]=translate_delegate_usage(best_input[0:3])
#translatedU[3]=(best_input[3])

X_list =[]
X_list2 =[]
X_list2.append( list(translatedU))
with open ("Bayesian OUTPUT"+str(x)+".csv", "a")  as out:
               out.write(str(best_input[0])+","+str(X_list2[0]))




# Plot convergence and save as a .png file
problem.plot_convergence(filename='convergence_plot')
