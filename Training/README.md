# Third Party Dependencies for Training a Custom Object Detector
To start training a custom object detectors, install the following dependencies in the given order.

## Installation Order

**1.) CUDA 9.0**

CUDA is used for GPU acceleration when training a custom object detector.  Follow instructions to install [CUDA here.](https://github.com/akirademoss/SnapCrack/blob/master/thirdparty/CUDA_Install.md) 

**2.) Tensorflow r1.12**

Tensorflow is used to create custom object detectors.  Follow the instructions to install [Tensorflow here.](https://github.com/akirademoss/SnapCrack/blob/master/thirdparty/TF_Install.md)


**3.) OpenCV v4.0.0**

OpenCV is used to visualize the 2D bounding boxes of our objects.  Follow the instructions to install [OpenCV here.](https://github.com/akirademoss/SnapCrack/tree/master/thirdparty/OpenCV%20Install%20Docs) Note that I did not detail installation instructions as thoroughly here.  [this link](https://github.com/akirademoss/SnapCrack/blob/master/thirdparty/OpenCV%20Install%20Docs/Ubuntu%2018.04_%20How%20to%20install%20OpenCV%20-%20PyImageSearch.pdf) provides the basic installation steps and [this link](https://github.com/akirademoss/SnapCrack/blob/master/thirdparty/OpenCV%20Install%20Docs/OpenCV%20Install%20for%20Deep%20Learning.pdf) provides the modified steps needed for installalling OpenCV v4.0.0 on Ubuntu 18.04

## Tutorials

**1.) Training a mobilenet object detector with Tensorflow**

[Training a mobilenet object detector with Tensorflow](https://git.ece.iastate.edu/sd/sdmay20-18/-/blob/master/Training/Dataset%20Tools%20&%20Object%20Detection.pdf) Train a custom lightweight object detector to run on Android
