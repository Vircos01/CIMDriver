import re

def keep_both(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    pattern = re.compile(r'<<<<<<< HEAD\n(.*?)\n=======\n(.*?)\n>>>>>>> origin/main\n', re.DOTALL)
    resolved = pattern.sub(lambda m: m.group(1) + "\n" + m.group(2) + "\n", content)
    with open(filepath, 'w') as f:
        f.write(resolved)

def keep_head(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    pattern = re.compile(r'<<<<<<< HEAD\n(.*?)\n=======\n(.*?)\n>>>>>>> origin/main\n', re.DOTALL)
    resolved = pattern.sub(lambda m: m.group(1) + "\n", content)
    with open(filepath, 'w') as f:
        f.write(resolved)

def custom_tracking_service():
    filepath = 'android/app/src/main/java/com/cimdriver/app/service/TrackingService.kt'
    with open(filepath, 'r') as f:
        content = f.read()
    
    # 1st conflict: keep HEAD (ZeppCompanionServer)
    content = re.sub(r'<<<<<<< HEAD\n\s*ZeppCompanionServer.stopIfNeeded\(\)\n\n=======\n\s*\n>>>>>>> origin/main\n', 
                     '                    ZeppCompanionServer.stopIfNeeded()\n', content)
    
    # 2nd conflict: keep main (liters)
    content = re.sub(r'<<<<<<< HEAD\n\s*litersPurchased = 0.0,\n=======\n\s*liters = 0.0,\n>>>>>>> origin/main\n',
                     '                    liters = 0.0,\n', content)
                     
    with open(filepath, 'w') as f:
        f.write(content)

keep_both('android/app/src/main/java/com/cimdriver/app/ui/navigation/NavRoutes.kt')
keep_head('android/app/build.gradle.kts')
keep_head('android/gradle/libs.versions.toml')
custom_tracking_service()
