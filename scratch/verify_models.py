import tensorflow as tf
import sys

def print_model_details(path):
    print(f"Loading {path}...")
    try:
        interpreter = tf.lite.Interpreter(model_path=path)
        interpreter.allocate_tensors()
        output_details = interpreter.get_output_details()
        print(f"Output shape: {output_details[0]['shape']}")
        print(f"Output type: {output_details[0]['dtype']}")
    except Exception as e:
        print(f"Error loading {path}: {e}")

if __name__ == '__main__':
    print_model_details('dentnova_mobilenetv2.tflite')
    print_model_details(r'C:\Users\Sharmila\Downloads\dentnova_mobilenetv2.tflite')
