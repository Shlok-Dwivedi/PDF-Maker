# Nothing Lens - Document Scanner & PDF Maker

A native Android document scanner and PDF maker crafted with Nothing OS design aesthetics. Built specifically for clean edge detection, auto-cropping, and multi-page document PDF export — just like the classic Office Lens.

---

## ⚡ Highlights & Features

- **Office Lens-Style Auto-Crop & Edge Detection**: Powered by Google's native ML Kit Document Scanner API (`play-services-mlkit-document-scanner`).
  - **Real-time Boundary Detection**: Live animated highlight of document borders.
  - **Auto-Shutter Capture**: Automatically takes the shot when the document is steady.
  - **Manual 4-Point Corner Adjustments**: Drag corner handles with precision magnifying loupes.
  - **Perspective De-warping**: Flattens skewed photos of documents into rectangular pages.
  - **Scanner Modes**: Full scanning flow with Clean, B&W, Grayscale, and Color filters.
- **In-App Gallery Import**: Select existing photos directly inside the scanner or using the built-in Gallery Studio.
- **In-App PDF Studio**:
  - Reorder pages
  - 90° page rotation
  - Contrast threshold filters (B&W Doc, Grayscale, Color boost)
  - Standard printable A4 sizing or original image aspect ratio
- **Built-in Document Manager**:
  - Browse scanned PDFs with thumbnail previews, page counts, and file sizes.
  - One-tap viewing and sharing via Nothing Quick Share, WhatsApp, Gmail, or Drive.
- **Nothing OS Aesthetic**: Monochromatic dark theme, red accent dot, dot-matrix typographic accents, and fluid Jetpack Compose UI.

---

## 📱 How to Install on Your Nothing Phone

The compiled APK is ready at:
`e:\Desktop\PDF Maker\NothingLens-v1.0.apk`

### Method 1: Download via Wi-Fi (No Cables Needed)
1. Run the included server on your PC:
   ```powershell
   node serve-apk.js
   ```
2. Open Chrome on your Nothing Phone (connected to the same Wi-Fi) and navigate to:
   ```
   http://10.141.10.228:8080/
   ```
3. Tap **Download APK** and tap **Install**.

### Method 2: Transfer via USB or Quick Share
1. Connect your Nothing Phone via USB cable or use Windows Quick Share.
2. Copy `NothingLens-v1.0.apk` to your phone's **Downloads** folder.
3. Open the **Files** app on your phone, tap `NothingLens-v1.0.apk`, and tap **Install**.
*(If prompted, allow "Install unknown apps" in Settings).*

---

## 🛠️ Tech Stack
- **OS**: Android (minSdk 26, targetSdk 34)
- **Language**: Kotlin 1.9.22
- **UI**: Jetpack Compose & Material 3
- **Vision & Scanning**: Google Play Services ML Kit Document Scanner
- **PDF Generation**: Native Android `android.graphics.pdf.PdfDocument` + `PdfRenderer`
- **Image Loading**: Coil 2.6.0
