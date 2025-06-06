'''
@@ This code has the function of automate HBO trigger and connects with Java server/client correctly
This is the main python client that has bayesian
    
You need to change "num_tasks" and "max_iter" here and also reflct the changes in two other codes:
    1-TwoClientsServer_new  of the Java code
    2-max_iteration  of TwoClientsServer_new (server)
    3-max_iteration in MainActivity of the Java code (app)
    
Instruction:
    @@ on this code
    1. Run the Pyhton
    
    @@ on the Android do this first: 
      
    2. Run server in Android java -cp ./ server.TwoClientsServer_new
    3. Push server Button on the app
    4. Add AI and Run them on the app, and at least on virtual object on the screen
    
    


'''
import os

import json
import socket
from GPyOpt.methods import BayesianOptimization
import random
import numpy as np
import matplotlib.pyplot as plt
import time
# Get the current time in seconds since the epoch
import datetime



#as client_socket:

 #client_socket.connect(('127.0.0.1', 4444))


def quick_test(X):
    #x1, x2, x3, x4 = X[:, 0], X[:, 1], X[:, 2],X[:, 3]
    for i in range(0,3):
      print( "x"+str(i+1)+": "+ str(X[0][i]))
      print( "4x"+str(i+1)+": "+ str(X[0][i]*4))
      print( "ROUND"+str(i+1)+": "+ str(round(X[0][i]*4)))
    X_list = X.tolist()
   
    print( "input: "+ str(X_list))
 
    return X[0][2]





def objective(X):
    
        current_time= datetime.datetime.now()
        timecur = current_time.strftime("%H:%M:%S.%f")
        with open (str(x)+"/Candidate_time"+str(x)+".csv", "a")  as out:
           out.write(str(timecur)+"\n")    
           
        global counter
        counter+=1
        translatedU=[0,0,0,0]
        translatedU[0:3]=translate_delegate_usage(X[0][0:3])
        translatedU[3]=(X[0][3])
    
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
        #global client_socket
          client_socket.connect(('127.0.0.1', 4444))
            #connected = client_socket.recv(1024).decode()
            #print("Recieved! "+ str(connected))
            
            
          client_socket.sendall(json_data.encode()+ b'\n') ##'\n' is a must Add a newline character very important@@@@ this is correct
           
          print(  str(counter)+": "+str(nontranslated_X_list[0])+" is sent, waiting for the reward ...")
          print(  " delegate is translated to", str(X_list[0]))
      
           
            #Receive the reward value from the Java server
          received_data = client_socket.recv(1024).decode()
          print("Recieved reward : "+ str(received_data))
          current_time= datetime.datetime.now()
          x_time = current_time.strftime("%H:%M:%S.%f")
          with open (str(x)+"/Bayesian OUTPUT"+str(x)+".csv", "a")  as out:
               out.write(str(x_time)+","+str(nontranslated_X_list[0])+","+str(X_list[0])+","+str(received_data))
        
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
  
    
    def __init__(self, domain,constraints  ):
     #super(JavaRewardBayesianOptimization, self).__init__(reward_function,domain=domain,constraints = constraints)
   
    # runs the exploreation phase for 5 times!
       super(JavaRewardBayesianOptimization, self).__init__( f=objective,domain=domain,constraints = constraints,acquisition_type ='EI')
       #super(JavaRewardBayesianOptimization, self).__init__( f=quick_test,domain=domain,constraints = constraints,acquisition_type ='EI')
                                                   

space = [     {'name': 'var_1', 'type': 'continuous', 'domain': [0,1]},
     {'name': 'var_2', 'type': 'continuous', 'domain': [0,1]},
     {'name': 'var_3', 'type': 'continuous', 'domain': [0,1]},
     {'name': 'var_4', 'type': 'continuous', 'domain':(0.2,1), 'dimensionality' :1}]         

current_time= datetime.datetime.now()
x = current_time.strftime("%H:%M")
os.makedirs(x, exist_ok=True)
timecur = current_time.strftime( "%H:%M:%S.%f")

print(x)
counter=0

#problem = JavaRewardBayesianOptimization(domain=space,constraints= [{'name': 'constr_1','constraint': 'x[:,0]+ x[:,1]+ x[:, 2] -1'},
#                                                     {'name': 'constr_2','constraint': '-x[:,0]- x[:,1]- x[:, 2] +0.999'}                              ])


 
while(1) :        
 with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client_socket:   
   client_socket.connect(('127.0.0.1', 4444))
   #global client_socket
   received_data = client_socket.recv(1024).decode()
   print("Recieved reward : "+ str(received_data))
   received_data=received_data.strip()
 
   if(str(received_data)=='activate'):

     with open (str(x)+"/Bayesian OUTPUT"+str(x)+".csv", "a")  as out:
         out.write("time,,cu,,gu,,nu,,tris,,,ct,,gt,,nt,,ttris,,reward \n")
    
    

     with open (str(x)+"/Candidate_time"+str(x)+".csv", "a")  as out:
         out.write("time \n")    
    
     num_tasks=6
     #num_tasks=3



     # Initialize the custom optimization class with the Java reward function
     problem = JavaRewardBayesianOptimization(domain=space,constraints= [{'name': 'constr_1','constraint': 'x[:,0]+ x[:,1]+ x[:, 2] -1'},
                                                          {'name': 'constr_2','constraint': '-x[:,0]- x[:,1]- x[:, 2] +0.999'}
                                                          
                                                          
                                                                    ])


     #problem = JavaRewardBayesianOptimizatio gos for exploring 5 different options and in the next lines we will run optimization
     #to exploit

    
     print("Initial exploration is finished!")
     max_iter =3
     #=6
     #10
#15
     problem.run_optimization(max_iter = max_iter)                                      
                

     best_input = problem.x_opt
     best_reward = problem.fx_opt

#upper confidence bounds by default has a good  convergence rate


     print("Best input combination: for iteration count #" +str(max_iter), best_input)
     print("Best reward:", best_reward)
     with open (str(x)+"/Bayesian OUTPUT"+str(x)+".csv", "a")  as out:
         out.write( str( best_input) +","+str( best_reward)+"\n")
     #This is the application of the best input to our app after the max trial

     translatedU=[0,0,0,0,0]
     translatedU[0:3]=translate_delegate_usage(best_input[0:3])
     translatedU[3]=(best_input[3])
     translatedU[4]=best_reward*-1

     X_list =[]
     X_list2 =[]
     X_list2.append( list(translatedU))
     X_list.append( list([best_reward*-1]))
 
        # Prepare data to send to the Java server
     data = {
           "python_client"
            : 
             X_list2[0] # this is to apply the changes to 
                #X_list[0] # this is for sending reward for baseline experiments to know the avg latency and quality
     }

        # Convert data to JSON string
     json_data = json.dumps(data)
# @@@@@@@@@ UNCOMMENT THIS

     with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client_socket:
            client_socket.connect(('127.0.0.1', 4444))         
            client_socket.sendall(json_data.encode()+ b'\n') ##'\n' is a must Add a newline character very important@@@@ this is correct
          
            print(  " delegate is translated to", str(X_list2[0]))
          
            #
            #Receive the reward value from the Java server
            received_data = client_socket.recv(1024).decode()
            print("Recieved reward : "+ str(received_data))
            #
            with open (str(x)+"/Bayesian OUTPUT"+str(x)+".csv", "a")  as out:
               out.write(str(best_input[0])+","+str(X_list2[0]))

 
     # Plot convergence and save as a .png file
     problem.plot_convergence(filename=str(x)+str("/")+'convergence_plot')
     problem.save_report(str(x)+"/report"+str(x))


