
Welcome to the HBO project!

This project is aimed at improving the performance of AI and AR by controlling virtual objects' triangle count and the AI computing unit.



You need to have Android Studio installed on your system. Then, you can download and install the android app on your phone. The app consists of two modules, AR and AI. In the AI module, some AI tasks are run some inferences on input of camera frames. To get it run, please first make sure to store chair.jpg in the local storage of your phone in this directory : Storage/emulated/0
One easy way to do it is just to copy and paste the image to the main (home) directory of your phone. Next,check the orange box on the left side of the app menu. First select the AI request number which is visible as tasks. In the middle column you can switch between the AI models, and in the last column, you can select either of the devices for inference (CPU, GPU, or NNAPI). The results of AI module are stored in response_t.csv at the local storage of your phone, following the project direction : Storage/emulated/0/Android/data/com.arcore.HBO_Bayesian/files

To use the AR module, you can select the virtual objects from the menu and put them on the screen. The result for AR module, i.e., the overall triangle count of the objects and the GPU usage is stored at GPU_Usage.cvs.
The current version of the app uses the decimated objects from the local storage, so you need to have the decimated version of all the used virtual objects on your phone in the same directory. (Storage/emulated/0/Android/data/com.arcore.HBO_Bayesian/files).
To generate the decimated version of the objects, you could use the python program provided in "Files related to decimating and converting OBJ to SFB" folder of the project and follow the guide from that directory.
You can see below a screenshot of the app.


![demo](https://user-images.githubusercontent.com/27611369/193911248-1e15edce-d97e-427f-8963-8b02638429ce.jpg)
