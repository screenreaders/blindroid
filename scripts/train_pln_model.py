import os
import sys
from pathlib import Path

try:
    import tensorflow as tf
except Exception as exc:
    print("TensorFlow is required to train the model.")
    print("Install with: pip install tensorflow==2.14.0")
    sys.exit(1)

DATA_DIR = Path(os.environ.get("PLN_DATA_DIR", "data/nbp_pln_raw"))
ASSETS_DIR = Path("app/src/main/assets")
MODEL_PATH = ASSETS_DIR / "pln_model.tflite"
LABELS_PATH = ASSETS_DIR / "pln_labels.txt"

if not DATA_DIR.exists():
    print(f"Dataset directory not found: {DATA_DIR}")
    sys.exit(1)

ASSETS_DIR.mkdir(parents=True, exist_ok=True)

IMG_SIZE = (224, 224)
BATCH = 8
SEED = 42
EPOCHS = 6
FINE_TUNE_EPOCHS = 3

train_ds = tf.keras.utils.image_dataset_from_directory(
    DATA_DIR,
    labels="inferred",
    label_mode="int",
    image_size=IMG_SIZE,
    batch_size=BATCH,
    shuffle=True,
    seed=SEED,
    validation_split=0.2,
    subset="training",
)
val_ds = tf.keras.utils.image_dataset_from_directory(
    DATA_DIR,
    labels="inferred",
    label_mode="int",
    image_size=IMG_SIZE,
    batch_size=BATCH,
    shuffle=True,
    seed=SEED,
    validation_split=0.2,
    subset="validation",
)

class_names = train_ds.class_names

with LABELS_PATH.open("w", encoding="utf-8") as f:
    for name in class_names:
        f.write(name + "\n")

normalization_layer = tf.keras.layers.Rescaling(1.0 / 255)

augmentation = tf.keras.Sequential([
    tf.keras.layers.RandomRotation(0.08),
    tf.keras.layers.RandomZoom(0.1),
    tf.keras.layers.RandomContrast(0.2),
    tf.keras.layers.RandomTranslation(0.08, 0.08),
])

base = tf.keras.applications.MobileNetV2(
    input_shape=IMG_SIZE + (3,),
    include_top=False,
    weights="imagenet",
)
base.trainable = False

inputs = tf.keras.Input(shape=IMG_SIZE + (3,))
x = normalization_layer(inputs)
x = augmentation(x)
x = tf.keras.applications.mobilenet_v2.preprocess_input(x)
x = base(x, training=False)
x = tf.keras.layers.GlobalAveragePooling2D()(x)
x = tf.keras.layers.Dropout(0.2)(x)
outputs = tf.keras.layers.Dense(len(class_names), activation="softmax")(x)
model = tf.keras.Model(inputs, outputs)

model.compile(
    optimizer=tf.keras.optimizers.Adam(1e-3),
    loss=tf.keras.losses.SparseCategoricalCrossentropy(),
    metrics=["accuracy"],
)

model.fit(train_ds, validation_data=val_ds, epochs=EPOCHS)

# Fine-tune last layers
base.trainable = True
for layer in base.layers[:-20]:
    layer.trainable = False

model.compile(
    optimizer=tf.keras.optimizers.Adam(1e-5),
    loss=tf.keras.losses.SparseCategoricalCrossentropy(),
    metrics=["accuracy"],
)

model.fit(train_ds, validation_data=val_ds, epochs=FINE_TUNE_EPOCHS)

converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]

try:
    tflite_model = converter.convert()
except Exception as exc:
    print(f"TFLite conversion failed: {exc}")
    sys.exit(1)

MODEL_PATH.write_bytes(tflite_model)
print(f"Saved TFLite model to {MODEL_PATH}")
print(f"Saved labels to {LABELS_PATH}")
