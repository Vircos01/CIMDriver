import urllib.request
import os

url = "https://www.cimsolutions.nl/wp-content/themes/stuurlui/assets/img/logo-footer.svg"
svg_path = "scratch/logo.svg"
urllib.request.urlretrieve(url, svg_path)

try:
    import cairosvg
except ImportError:
    os.system("pip install cairosvg")
    import cairosvg

cairosvg.svg2png(url=svg_path, write_to="app/src/main/res/drawable/cimsolutions_logo.png", scale=3.0)
print("Logo downloaded and converted!")
