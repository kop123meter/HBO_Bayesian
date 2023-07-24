#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Fri Jul 21 19:11:04 2023

@author: niloofar
"""

import socket

# Create a TCP/IP socket
client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Connect to the server
client_socket.connect(('127.0.0.1', 4444))

# Your data to send (replace this with your actual data)
data2 = "Hello from Python client!"

# Convert data to bytes and prepend with the length of the message
data_to_send = len(data2).to_bytes(4, byteorder='big') + data2.encode()

# Send data to the Java server
client_socket.sendall(data_to_send)

# Close the socket
client_socket.close()
