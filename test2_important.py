#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Fri Jul 21 19:11:04 2023

@author: niloofar
"""
import json
import socket
from GPyOpt.methods import BayesianOptimization
import random
import socket

# Create a TCP/IP socket
client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Connect to the server
client_socket.connect(('127.0.0.1', 4444))

# Your data to send (replace this with your actual data)
data = {
     "python_client"
      : 
          [0.3,0.7,0,3000]
  }


  # Convert data to JSON string
json_data = json.dumps(data)

# Convert data to bytes and prepend with the length of the message
client_socket.sendall(json_data.encode()) #this is correct
print("sent 2 and waiting for recieving ")

received_data = client_socket.recv(1024).decode()
print("Recieved! " + str(received_data))

# Close the socket
client_socket.close()
