import os
import argparse
from PIL import Image
import cv2 as cv
import tkinter
import tkinter.constants
import tkinter.filedialog

while True:
    print("Please select your image directory.")
    input_dir = tkinter.filedialog.askdirectory()
    if input_dir == None or input_dir == "":
        print("You must select a directory.")
        continue
    break

DEFAULT_DMN = (0, 0, 600, 600)

def crop_image(input_dir, infile, output_dir="cropped", dmn=DEFAULT_DMN):
    outfile = os.path.splitext(infile)[0] 
    extension = os.path.splitext(infile)[1]
    
    try:
        img = Image.open(input_dir + '/' + infile)
        img = img.crop((dmn[0], dmn[1], dmn[2], dmn[3]))

        new_file = output_dir + "/" + outfile + extension
        img.save(new_file)
    except IOError:
        print("unable to resize image {}".format(infile))

if __name__ == "__main__":
    dir = os.getcwd()

    parser = argparse.ArgumentParser()
    #parser.add_argument('-i', '--input_dir', help='Full Input Path')
    parser.add_argument('-o', '--output_dir', help='Full Output Path')

    parser.add_argument('-l', '--left', help='Resized Width')
    parser.add_argument('-t', '--top', help='Resized Width')
    parser.add_argument('-r', '--right', help='Resized Height')
    parser.add_argument('-b', '--bottom', help='Resized Height')

    args = parser.parse_args()

    #if args.input_dir:
    #    input_dir = args.input_dir
    #else:
    #    input_dir = dir + '/images'

    if args.output_dir:
        output_dir = args.output_dir
    else:
        output_dir = dir + '/cropped'

    if args.left and args.top and args.right and args.bottom:
        dmn = (int(args.left), int(args.top), int(args.right), int(args.bottom))
    else:
        dmn = DEFAULT_DMN

    if not os.path.exists(os.path.join(dir, output_dir)):
        os.mkdir(output_dir)

    try:
        for file in os.listdir(input_dir):
            crop_image(input_dir, file, output_dir, dmn=dmn)
    except OSError:
        print('file not found')
