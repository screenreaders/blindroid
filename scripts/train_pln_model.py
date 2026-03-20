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
EXTRA_DIR = Path(os.environ.get("PLN_DATA_EXTRA", "data/nbp_pln_aug"))
ASSETS_DIR = Path("app/src/main/assets")
MODEL_PATH = ASSETS_DIR / "pln_model.tflite"
LABELS_PATH = ASSETS_DIR / "pln_labels.txt"

if not DATA_DIR.exists():
    print(f"Dataset directory not found: {DATA_DIR}")
    sys.exit(1)

ASSETS_DIR.mkdir(parents=True, exist_ok=True)

IMG_SIZE = (224, 224)
BATCH = int(os.environ.get("PLN_BATCH", "8"))
SEED = 42
EPOCHS = int(os.environ.get("PLN_EPOCHS", "10"))
FINE_TUNE_EPOCHS = int(os.environ.get("PLN_FINE_TUNE_EPOCHS", "5"))

data_dirs = [DATA_DIR]
if EXTRA_DIR.exists():
    data_dirs.append(EXTRA_DIR)

class_names = sorted([p.name for p in DATA_DIR.iterdir() if p.is_dir()])
if not class_names:
    print(f"No class folders found in {DATA_DIR}")
    sys.exit(1)

def build_dataset(path: Path, subset: str):
    return tf.keras.utils.image_dataset_from_directory(
        path,
        labels="inferred",
        label_mode="int",
        class_names=class_names,
        image_size=IMG_SIZE,
        batch_size=BATCH,
        shuffle=True,
        seed=SEED,
        validation_split=0.2,
        subset=subset,
    )

train_ds = None
val_ds = None
for data_dir in data_dirs:
    dir_train = build_dataset(data_dir, "training")
    dir_val = build_dataset(data_dir, "validation")
    train_ds = dir_train if train_ds is None else train_ds.concatenate(dir_train)
    val_ds = dir_val if val_ds is None else val_ds.concatenate(dir_val)

AUTOTUNE = tf.data.AUTOTUNE
train_ds = train_ds.prefetch(AUTOTUNE)
val_ds = val_ds.prefetch(AUTOTUNE)

with LABELS_PATH.open("w", encoding="utf-8") as f:
    for name in class_names:
        f.write(name + "\n")

augmentation = tf.keras.Sequential([
    tf.keras.layers.RandomFlip("horizontal"),
    tf.keras.layers.RandomRotation(0.08),
    tf.keras.layers.RandomZoom(0.1),
    tf.keras.layers.RandomContrast(0.25),
    tf.keras.layers.RandomBrightness(0.2),
    tf.keras.layers.RandomTranslation(0.1, 0.1),
    tf.keras.layers.GaussianNoise(0.02),
])

base = tf.keras.applications.MobileNetV2(
    input_shape=IMG_SIZE + (3,),
    include_top=False,
    weights="imagenet",
)
base.trainable = False

inputs = tf.keras.Input(shape=IMG_SIZE + (3,))
x = tf.keras.layers.Rescaling(1.0 / 255)(inputs)
x = augmentation(x)
x = tf.keras.layers.Rescaling(2.0, offset=-1)(x)
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
