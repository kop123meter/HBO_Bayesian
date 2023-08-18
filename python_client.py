import json
import socket
from GPyOpt.methods import BayesianOptimization
import random

'''
def reward_function(x):
       # Calculate the reward based on the input combination x
       # Replace this with your actual reward calculation
       x1, x2, x3,x4 = x[:, 0], x[:, 1], x[:, 2], x[:, 3],
       reward= (x1+x2 )*x3
       #reward= get_result_element(input_data,  [x1,x2,x3,x4])
       return (reward*-1)
'''
import time

# Get the current time in seconds since the epoch


def objective( X):
        # Convert X to a list (assuming X is a numpy array)
        X_list = X.tolist()

        # Prepare data to send to the Java server
        data = {
           "python_client"
            : 
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
           
            
            print(  str(X_list)+" is sent, waiting for the reward ...")
          
           
            #Receive the reward value from the Java server
            #print(" Is receiving! ")
            received_data = client_socket.recv(1024).decode()
            print("Recieved reward : "+ str(received_data))
            
            with open ("Bayesian OUTPUT.txt", "a")  as out:
               out.write(str(X_list)+" is sent, waiting for the reward ...")
               out.write("Recieved reward : "+ str(received_data))

    

        return (float(received_data)*-1) # this should pass the reward function from java server after calculating the mean quality and average AI inference response time


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
        super(JavaRewardBayesianOptimization, self).__init__(objective,domain=domain,constraints = constraints)
                                                 
                               
   




space = [{'name': 'var_1', 'type': 'discrete', 'domain': (0, 0.3, 0.7, 1)},
          {'name': 'var_2', 'type': 'discrete', 'domain': (0, 0.3, 0.7, 1)},
         {'name': 'var_3', 'type': 'discrete', 'domain': (0, 0.3, 0.7, 1)},
         {'name': 'var_4', 'type': 'continuous', 'domain':(7.3,73), 'dimensionality' :1},# for 5 splanes
        #{'name': 'var_4', 'type': 'continuous', 'domain':(0.28,0.62), 'dimensionality' :1},# for 4 andies
        #{'name': 'var_4', 'type': 'discrete', 'domain':  (0.435, 0.217, 0.143)}
          ]


#problem = GPyOpt.methods.BayesianOptimization(reward_function, domain=space , constraints= [{'name': 'constr_1', 'constraint': 'np.abs(x[:, 0] + x[:, 1] +x[:, 2] - 1) '}])


with open ("Bayesian OUTPUT.txt", "w")  as out:
    out.write("NEW Bayeian Test \n")


# Initialize the custom optimization class with the Java reward function
problem = JavaRewardBayesianOptimization(domain=space,constraints= [{'name': 'constr_1', 'constraint': 'np.abs(x[:, 0] + x[:, 1] +x[:, 2] - 1) '}])


'''problem = JavaRewardBayesianOptimizatio gos for exploring 5 different options and in the next lines we will run optimization
to exploit'''

    
print("Initial exploration is finished!")
max_iter  = 10
problem.run_optimization(max_iter = max_iter)                                      
                

problem.plot_acquisition()
problem.plot_convergence()
best_input = problem.x_opt
best_reward = problem.fx_opt

#upper confidence bounds by default has a good  convergence rate


print("Best input combination: for iteration #" +str(max_iter), best_input)
print("Best reward:", best_reward)





    #, constraints=[{'name': 'constr_1', 'constraint': 'np.abs(x[:, 0] + x[:, 1] +x[:, 2] - 1)'}])
with open ("Bayesian OUTPUT.txt", "a")  as out:
    out.write("Best input combination: for iteration #" +str(max_iter)+ str( best_input))
    out.write("Best reward:"+str( best_reward))
    