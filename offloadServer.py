import socket
import struct
import numpy as np
import tensorflow as tf
from tensorflow.lite.python.interpreter import Interpreter

# load_model, process_image, and socket_server functions
def load_model(model_path):
    interpreter = Interpreter(model_path=model_path)
    interpreter.allocate_tensors()
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    return interpreter, input_details, output_details
# refrence to ChatGPT 
def process_image(interpreter, input_details, output_details, img_data):
    # process image data to input tensor as model expects
    img = np.frombuffer(img_data, dtype=np.float32).reshape(input_details[0]['shape'])

    # set input tensor
    interpreter.set_tensor(input_details[0]['index'], img)
    
    # run inference
    interpreter.invoke()

    # get output tensor
    segmap = interpreter.get_tensor(output_details[0]['index'])
    
    return segmap

def socket_server(model_path):


    #  we will use the load_model and process_image functions to load the model and process the image data
    # so that we can run inference on the image data and generate the segmentation map


    interpreter, input_details, output_details = load_model(model_path)

    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    print("Socket created")
    server_socket.bind(('0.0.0.0', 45455))
    server_socket.listen(5)
    print("Server listening on port 45455")

    while True:
        client_socket, addr = server_socket.accept()
        print(f"Connection from {addr}")

        try:
            # receive image data size
            img_data_size = struct.unpack('>I', client_socket.recv(4))[0]

            # receive image data
            img_data = bytearray()
            while len(img_data) < img_data_size:
                packet = client_socket.recv(4096)
                if not packet:
                    break
                img_data.extend(packet)

            # process image data and get segmentation map
            segmap = process_image(interpreter, input_details, output_details, img_data)

            # convert segmap to bytes since we will send it to the client
            # Via socket, we can only send bytes
            segmap_bytes = segmap.astype(np.float32).tobytes()

            # place the size of the segmap bytes in a 4-byte header
            # so that the client knows how many bytes to expect
            client_socket.sendall(struct.pack('>I', len(segmap_bytes)))

            # send the segmap bytes
            # we can send the segmap bytes in chunks of 4096 bytes
            # so that we can send the segmap bytes in multiple packets
            client_socket.sendall(segmap_bytes)

        except Exception as e:
            print(f"Error: {e}")
        finally:
            client_socket.close()
            print(f"Connection closed with {addr}")

if __name__ == '__main__':
    model_path = '/Users/lize/Documents/GitHub/HBO_Bayesian/app/src/main/assets/deconv_fin_munet.tflite'
    socket_server(model_path)
