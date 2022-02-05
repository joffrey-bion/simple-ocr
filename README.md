# Simple OCR

A simple OCR for Kotlin that recognizes single-line text in small images.
It is useful in applications where the images are very predictable
(in particular using the same font size, font family and color).

## Simple and naive algorithm

This OCR doesn't use machine learning to detect text.
Instead, it recognizes characters in an image by breaking in up into sub-images,
and comparing those sub-images to a set of labeled reference images (1 or 2 characters each).

The text detection is based on a `ColorFilter`, which decides if a pixel is part of the text or not.
The default one, `ColorSimilarityFilter`, uses the similarity to a reference color (within a given tolerance) to decide.

### 1. Split into sub-images

The image is first split in vertical slices delimited by the columns of pixels that don't have any text.

> This is roughly splitting the text into individual characters, although sometimes several characters can be in the 
same sub-image because there is not even a single column of non-text pixels between them (due to kerning).
> 
> For instance, in Arial, a lowercase letter following an uppercase T ou V can be part of a single sub-image (Te, To, Va...)
because the T or V goes above the next letter and prevents empty columns.

These sub-images are returned along with the "blank" delimiter widths, so the OCR can later decide what constitutes a
simple character separation, or an actual blank space.

### 2. Trim sub-images

If this feature enabled (which is the case by default), the sub-images are trimmed vertically by eliminating the rows
of pixels at the top and bottom that don't contain any text.

This is done so that the text can be in a slightly different vertical position in the images, and still give consistent
sub-images.
If the text is guaranteed to be at the same vertical position in the images, you may disable this feature to be more
strict on the match (see the configuration in the Usage section).

### 3. Match with reference images

The actual recognition of characters is done by the `SimpleOcr`, which compares each sub-image to a set of provided 
(and labeled) reference images.
The most similar reference image "wins" and the sub-image is attributed the corresponding label
(character or group of characters).

The result is then composed by concatenating all the matched labels, and deciding where to put spaces depending
on the amount of blank space in the image.

## Preliminary setup

### Generate reference images

The first step is to prepare some reference images that you will use for the OCR.
In order to do that, get a hold on some sample images similar to the ones that you intend to apply the OCR to.

Use the `TextDetector` without OCR to split the sample images into reference sub-images:

```kotlin
import org.hildan.ocr.Color
import org.hildan.ocr.TextDetector
import kotlin.io.path.Path

fun main() {
    val inputDir = Path("./sampleImages")
    val outputDir = Path("src/main/resources/ocr/refImages")

    // This uses the default configuration, but you can customize further the color filter and various tolerances
    // Make sure you configure the TextDetector the same way as the one used by the OCR.
    val textDetector = TextDetector(textColor = Color.BLACK)

    textDetector.splitAndSaveSubImages(inputDir, outputDir)
}
```

There are other methods similar to `splitAndSaveSubImages` if you want to process images individually or want to have
more control over file names etc.

### Label reference images

The generated images need to be "labeled", which means you need to manually input what text they represent.
You can either do the mapping in the code explicitly, or use the filenames to do this automatically.

#### Option 1: labels in code

The generated reference images can be associated to their label directly from the code.
If you read those images from resources, you can use the `KClass.getResourceImage` helper:

```kotlin
import org.hildan.ocr.getResourceImage
import org.hildan.ocr.reference.ReferenceImage

private fun referenceImage(imgName: String, text: String) = ReferenceImage(
    image = MyClass::class.getResourceImage("/ocr/refImages/$imgName"),
    text = text,
)

val referenceImages = listOf(
    referenceImage(imgName = "a.png", text = "a"),
    referenceImage(imgName = "b.png", text = "b"),
    referenceImage(imgName = "c.png", text = "c"),
    // etc.
)
```

#### Option 2: filenames as labels

> :warning: This method should only be used if you read the reference images as files.
> Use the Option 1 if your images should be read from the JAR's resources.
> This limitation is due to the fact that we can't list resources under a "resources directory" when inside a JAR.

The generated sub-image files can automatically be used as a reference images using `ReferenceImages.readFrom(directory)`
if they are read from the file system.
In this case, the filename (without extension) is used as a "label" for the reference image.
You can therefore manually rename the image files according to the text the image represents.

The text in the filenames can be URL-encoded if the filesystem doesn't support some characters.
For instance, use `%3C` for the character `<`.

Also, anything following a space in the filename will be ignored for the label of the image.
This is useful if you want to map different images to the same text, or if your filesystem is case-insensitive,
because this way you can disambiguate lowercase and uppercase letters.
For instance, you cannot save `a.png` and `A.png` on Windows, but you can do `a.png` and `A uppercase.png`.
The method `ReferenceImages.readFrom` will ignore the space and the `uppercase` suffix, and consider `A` as the label.

## Usage

Once you have prepared the reference images, you can use the OCR this way:

```kotlin
import org.hildan.ocr.*
import org.hildan.ocr.reference.*
import java.awt.image.BufferedImage

val ocr = SimpleOcr(
    referenceImages = ReferenceImages.readFrom(Path("./ocr/refImages")),
    textColor = Color.BLACK, // use the color corresponding to the text in your images
)

val someImage: BufferedImage = TODO("read image from somewhere")

val text = ocr.recognizeText(someImage)
```

### Advanced configuration

You can of course further customize the settings for the text detection and the OCR:

```kotlin
val ocr = SimpleOcr(
    referenceImages = ReferenceImages.readFrom(Path("./ocr/refImages")),
    textDetector = TextDetector(
        textColorFilter = ColorSimilarityFilter(
            referenceColor = Color(0xFFE5D5E5u),
            rgbTolerance = 25,
            alphaTolerance = 0,
        ),
        trimSubImagesVertically = true,
    ),
)
```
