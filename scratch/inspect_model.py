import os
import numpy as np
import tensorflow as tf
from tensorflow.keras.preprocessing import image

print("Loading tflite model...")
interpreter = tf.lite.Interpreter(model_path="dentnova_mobilenetv2.tflite")
interpreter.allocate_tensors()
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print("Input shape:", input_details[0]['shape'])
print("Input type:", input_details[0]['dtype'])
print("Output shape:", output_details[0]['shape'])
print("Output type:", output_details[0]['dtype'])

CLASS_NAMES = ["Calculus", "Gingivitis", "Healthy", "Invalid"]

def test_image(img_path_or_array):
    if isinstance(img_path_or_array, str):
        img = image.load_img(img_path_or_array, target_size=(224, 224))
        img_array = image.img_to_array(img) / 255.0
    else:
        img_array = img_path_or_array / 255.0
    
    img_array = np.expand_dims(img_array, axis=0).astype("float32")
    interpreter.set_tensor(input_details[0]["index"], img_array)
    interpreter.invoke()
    preds = interpreter.get_tensor(output_details[0]["index"])[0]
    
    class_index = int(np.argmax(preds))
    class_name = CLASS_NAMES[class_index]
    confidence = float(preds[class_index])
    print(f"\nPreds: {preds}")
    print(f"Top Class: {class_name} (index {class_index}), Confidence: {confidence:.4f}")
    return class_name, confidence, preds

print("\n--- Testing Tooth Image ---")
tooth_img_path = r"C:\Users\Sharmila\Downloads\tooth.png"
if os.path.exists(tooth_img_path):
    test_image(tooth_img_path)

print("\n--- Testing Solid Black Image ---")
black_img = np.zeros((224, 224, 3), dtype=np.float32)
test_image(black_img)

print("\n--- Testing Solid White Image ---")
white_img = np.ones((224, 224, 3), dtype=np.float32) * 255.0
test_image(white_img)

print("\n--- Testing Random Noise Image ---")
np.random.seed(42)
rand_img = np.random.randint(0, 256, (224, 224, 3), dtype=np.float32)
test_image(rand_img)
