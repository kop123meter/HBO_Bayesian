
Welcome to the HBO project!!!

Jointly managing virtual object triangle count and AI task allocation is a necessary, yet complex, strategy
to effectively trade off between virtual object quality and AI task latency. The proposed HBO framework provides an efficient solution to this problem.

![framework](https://github.com/Niloofar-didar/HBO_Bayesian/assets/27611369/35a22185-a844-4488-b8a8-e824e2baa94d)

First, it leverages Bayesian optimization (BO) to identify a solution for the continuous joint optimization variables related to AI resource usage and virtual objects triangle count ratio. This optimization process aims to minimize a black-box cost function for improved performance within a few exploratory steps. In the second stage, HBO employs heuristics to incorporate the candidate solutions into the system for cost evaluation, by adjusting AI allocation of each task and the triangle count of each virtual
object. We evaluate HBO with real smartphones and users against four different state-of-the-art baselines that provide static and dynamic AI allocation methods. Our results have shown that HBO helps reduce the average AI task latency by up to 3.5x and increase the average virtual object quality by up to 38.7% compared to the baselines.

Below is the image of our architecture and an screenshot of the app with some objects 


![architectureHBO](https://github.com/Niloofar-didar/HBO_Bayesian/assets/27611369/1136d5fd-d93f-4633-8d63-ec16c0a4adb2)



![close](https://github.com/Niloofar-didar/HBO_Bayesian/assets/27611369/29c9cd46-77fc-46e0-a1c1-122b47dc7afc)


*** The result of this project is accepted for publication in ICDCS Conference 2024.

Guidance to install and run the framework:
All You need to do is to clone the repository and run it on Android Studio. Then, you run the app and install the android app on your phone. The app consists of various TensorflowLite tasks for inference on camera frames such as mobilenet, mnist, efficientclass-lite0, model-metadata. You can add as many as models you want into it, but you need to adjust these three Kotlin classess that we define the model used as well as run inference function: AiItemsViewModel.kt and AiRecyclerviewAdapter.kt and BitmapCollector
Additionally, you can play with the app, change the task resource allocation and see the response time result, but if you want HBO to work, you need to push HBO button. For manually playing with task rellocation, check the orange box on the left side of the app menu. Then select the AI request number which is visible as tasks. In the middle column you can switch between the AI models, and in the last column, you can select either of the devices for inference (CPU, GPU, or NNAPI).

