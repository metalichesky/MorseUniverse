# This is a sample Python script.
import os
import time

import matplotlib.pyplot as plt
import numpy as np
import PIL
import pathlib
import random
import math
import tensorflow as tf

from tensorflow import keras
from tensorflow.keras import layers
from tensorflow.keras.models import Sequential

model_dir = pathlib.Path("C:\\Users\\Dmitriy\\Desktop\\model")
train_dir = pathlib.Path("D:\\train")
test_images_dir = pathlib.Path("C:\\Users\\Dmitriy\\Desktop\\test")


# Press Shift+F10 to execute it or replace it with your code.
# Press Double Shift to search everywhere for classes, files, tool windows, actions, and settings.

# def load_images_dirs():
#     directories = train_dir.glob('**/*')
#     data_dirs = [(x, x.name) for x in directories if x.is_dir()]
#     for dir in data_dirs:
#         print("dir {}".format(dir[0]))
#         print("label {}".format(dir[1]))
#     return train_dir, data_dirs


def print_versions():
    print("TensorFlow version: {}".format(tf.__version__))
    print("Eager execution: {}".format(tf.executing_eagerly()))
    # Use a breakpoint in the code line below to debug your script.


def show_images(dirs_with_labels):
    plt.figure(figsize=(10, 10))
    width = 3
    height = math.ceil(len(dirs_with_labels) / 3.0)
    for i in range(0, len(dirs_with_labels)):
        dir = dirs_with_labels[i]
        print("show image for label {}".format(dir[1]))
        images = list(dir[0].glob('**/*.jpg'))
        images_count = len(images)
        random_image = random.choice(images)
        print("show image {}".format(random_image))
        image = PIL.Image.open(str(random_image))
        ax = plt.subplot(height, width, i + 1)
        plt.imshow(image)
        plt.title(dir[1])
        plt.axis("off")
    plt.show()


def train_model():
    img_height = 180
    img_width = 180
    batch_size = 32
    train_split_ratio = 0.8
    validation_split_ratio = 1.0 - train_split_ratio
    # labels = list(map(lambda x: x[1], dirs_with_labels))
    # print(labels)

    train_ds = tf.keras.preprocessing.image_dataset_from_directory(train_dir,
                                                                   labels='inferred',
                                                                   color_mode='rgb',
                                                                   batch_size=batch_size,
                                                                   image_size=(img_width, img_height),
                                                                   shuffle=True,
                                                                   seed=random.randint(100, 1000),
                                                                   validation_split=validation_split_ratio,
                                                                   subset='training')
    validation_ds = tf.keras.preprocessing.image_dataset_from_directory(train_dir,
                                                                        labels='inferred',
                                                                        color_mode='rgb',
                                                                        batch_size=batch_size,
                                                                        image_size=(img_width, img_height),
                                                                        shuffle=True,
                                                                        seed=random.randint(100, 1000),
                                                                        validation_split=validation_split_ratio,
                                                                        subset='validation')

    # show_dataset_images_examples(train_ds)

    # show_dataset_images_examples(validation_ds)

    validation_ds_batches = tf.data.experimental.cardinality(validation_ds)
    test_ds_batches = validation_ds_batches // 5
    test_ds = validation_ds.take(test_ds_batches)
    validation_ds = validation_ds.skip(test_ds_batches)
    print('Number of validation batches: %d' % tf.data.experimental.cardinality(validation_ds))
    print('Number of test batches: %d' % tf.data.experimental.cardinality(test_ds))

    class_names = train_ds.class_names
    print("used class names: {}".format(class_names))
    num_classes = len(class_names)
    print("num of classes: {}".format(num_classes))

    AUTOTUNE = tf.data.AUTOTUNE
    train_ds = train_ds#.cache().shuffle(1500).prefetch(buffer_size=AUTOTUNE)
    validation_ds = validation_ds#.cache().prefetch(buffer_size=AUTOTUNE)
    test_ds = test_ds#.cache().prefetch(buffer_size=AUTOTUNE)
    print("datasets optimisations: caching and prefetching")

    print("images batch shape:")
    for image_batch, labels_batch in train_ds:
        print(image_batch.shape)
        print(labels_batch.shape)
        break

    # normalization layer to normalize color components
    normalization_layer = layers.experimental.preprocessing.Rescaling(1. / 255, input_shape=(img_height, img_width, 3))

    # manual normalization
    # normalized_ds = train_ds.map(lambda x, y: (normalization_layer(x), y))
    # image_batch, labels_batch = next(iter(normalized_ds))
    # first_image = image_batch[0]
    # # Notice the pixels values are now in `[0,1]`.
    # print(np.min(first_image), np.max(first_image))

    # use data augmentation to resolve overfitting problem
    data_augmentation_layers = keras.Sequential(
        [
            layers.experimental.preprocessing.RandomFlip("vertical",
                                                         input_shape=(img_height,
                                                                      img_width,
                                                                      3)),
            layers.experimental.preprocessing.RandomTranslation(height_factor=(0.0, 0.0),
                                                                width_factor=(0.0, 0.1),
                                                                fill_mode="wrap")
        ]
    )

    # show_data_augmentation_examples(train_ds, data_augmentation_layers)

    model = Sequential([
        normalization_layer,
        data_augmentation_layers,
        layers.Conv2D(16, 3, padding='same', activation='relu'),
        layers.MaxPooling2D(),
        layers.Conv2D(32, 3, padding='same', activation='relu'),
        layers.MaxPooling2D(),
        layers.Conv2D(64, 3, padding='same', activation='relu'),
        layers.MaxPooling2D(),
        layers.Dropout(0.2),
        layers.Flatten(),
        layers.Dense(128, activation='relu'),
        layers.Dense(num_classes)
    ])
    model.compile(optimizer='adam',
                  loss=tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True),
                  metrics=['accuracy'])

    model.summary()

    loss0, accuracy0 = model.evaluate(validation_ds)
    print("initial loss: {:.2f}".format(loss0))
    print("initial accuracy: {:.2f}".format(accuracy0))

    epochs = 1
    history = model.fit(
        train_ds,
        validation_data=validation_ds,
        epochs=epochs
    )

    loss, accuracy = model.evaluate(test_ds)
    print('Test accuracy :', accuracy)
    print('Test loss :', loss)

    t = time.time()
    export_path = model_dir / str(int(t))
    print('Export model into file {}'.format(export_path))
    model.save(export_path)

    show_training_history(history, epochs)


def show_training_history(history, epochs):
    acc = history.history['accuracy']
    val_acc = history.history['val_accuracy']

    loss = history.history['loss']
    val_loss = history.history['val_loss']

    plt.figure(figsize=(8, 8))
    plt.subplot(2, 1, 1)
    plt.plot(acc, label='Training Accuracy')
    plt.plot(val_acc, label='Validation Accuracy')
    plt.legend(loc='lower right')
    plt.ylabel('Accuracy')
    plt.ylim([min(plt.ylim()), 1])
    plt.title('Training and Validation Accuracy')

    plt.subplot(2, 1, 2)
    plt.plot(loss, label='Training Loss')
    plt.plot(val_loss, label='Validation Loss')
    plt.legend(loc='upper right')
    plt.ylabel('Cross Entropy')
    plt.ylim([0, 1.0])
    plt.title('Training and Validation Loss')
    plt.xlabel('epoch')
    plt.show()


def show_dataset_images_examples(dataset):
    class_names = dataset.class_names
    plt.figure(figsize=(10, 10))
    for images, labels in dataset.take(1):
        for i in range(9):
            ax = plt.subplot(3, 3, i + 1)
            plt.imshow(images[i].numpy().astype("uint8"))
            plt.title(class_names[labels[i]])
            plt.axis("off")
    plt.show()


def show_data_augmentation_examples(dataset, data_augmentation):
    for image, _ in dataset.take(1):
        plt.figure(figsize=(10, 10))
        first_image = image[0]
        for i in range(9):
            ax = plt.subplot(3, 3, i + 1)
            augmented_image = data_augmentation(tf.expand_dims(first_image, 0))
            plt.imshow(augmented_image[0] / 255)
            plt.axis('off')
    plt.show()


def load_model():
    models_dirs = list(model_dir.glob('**/*'))
    # if (len(models_dirs) <= 0)
    import_path = models_dirs[0]
    return tf.keras.models.load_model(str(import_path))


def test_model(model):
    img_height = 180
    img_width = 180
    all_dataset = tf.keras.preprocessing.image_dataset_from_directory(train_dir,
                                                                      labels='inferred',
                                                                      color_mode='rgb',
                                                                      image_size=(img_width, img_height))
    class_names = all_dataset.class_names

    test_images = list(test_images_dir.glob("**/*.jpg"))
    for test_image in test_images:
        test_image_path = str(test_image)
        img = keras.preprocessing.image.load_img(
            test_image_path, target_size=(img_height, img_width)
        )
        img_array = keras.preprocessing.image.img_to_array(img)
        img_array = tf.expand_dims(img_array, 0)  # Create a batch
        predictions = model.predict(img_array)
        score = tf.nn.softmax(predictions[0])
        print("{} image most likely belongs to {} class with a {:.2f} percent confidence."
              .format(str(test_image.name), str(class_names[np.argmax(score)]), 100 * np.max(score)))


def convert_model():
    models_dirs = list(model_dir.glob('**/*'))
    # if (len(models_dirs) <= 0)
    save_model_dir = str(models_dirs[0])
    save_model_dir_name = models_dirs[0].name
    converter = tf.lite.TFLiteConverter.from_saved_model(save_model_dir)  # path to the SavedModel directory
    tflite_model = converter.convert()
    print("lite model converted")

    lite_model_path = model_dir / (save_model_dir_name + '.tflite')
    # Save the model.
    with open(lite_model_path, 'wb') as f:
        f.write(tflite_model)
    print("lite model saved")


# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    # print_versions()
    # train_model()
    # convert_model()
    model = load_model()
    test_model(model)

# See PyCharm help at https://www.jetbrains.com/help/pycharm/
