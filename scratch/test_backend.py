import sys
sys.path.insert(0, '.')
import io
import os
import unittest
import numpy as np
from PIL import Image
from app import app

class BackendTestCase(unittest.TestCase):
    def setUp(self):
        app.testing = True
        self.client = app.test_client()

    def test_home(self):
        res = self.client.get('/')
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertTrue(data.get('success'))

    def test_health(self):
        res = self.client.get('/health')
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertEqual(data.get('status'), 'healthy')
        self.assertTrue(data.get('tooth_model'))

    def test_predict_questionnaire(self):
        sample = {
            "age": 25,
            "gender": 1,
            "brush_frequency": 2,
            "floss_frequency": 1,
            "sugar_intake": 2,
            "smoking": 0,
            "alcohol": 0,
            "bleeding_gums": 0,
            "tooth_sensitivity": 0,
            "last_dental_visit": 6
        }
        res = self.client.post('/predict', json=sample)
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertTrue(data.get('success'))
        self.assertIn('score', data)
        self.assertIn('risk', data)

    def test_predict_tooth_random_image(self):
        # Create a random noise image (non-tooth)
        img_array = np.random.randint(0, 256, (224, 224, 3), dtype=np.uint8)
        img = Image.fromarray(img_array)
        img_bytes = io.BytesIO()
        img.save(img_bytes, format='JPEG')
        img_bytes.seek(0)

        res = self.client.post(
            '/predict-tooth',
            content_type='multipart/form-data',
            data={'image': (img_bytes, 'random_image.jpg')}
        )
        data = res.get_json()
        print("\n[Random Image Response]:", res.status_code, data)
        self.assertEqual(res.status_code, 400)
        self.assertFalse(data.get('success'))
        self.assertEqual(data.get('message'), "Please upload a valid tooth image.")

if __name__ == '__main__':
    unittest.main()
