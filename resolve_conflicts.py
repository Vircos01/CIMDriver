import re

def resolve_by_keeping_both(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    # regex to match conflict markers
    pattern = re.compile(r'<<<<<<< HEAD\n(.*?)\n=======\n(.*?)\n>>>>>>> origin/main\n', re.DOTALL)
    
    def replacer(match):
        return match.group(1) + "\n" + match.group(2) + "\n"
        
    resolved = pattern.sub(replacer, content)
    
    with open(filepath, 'w') as f:
        f.write(resolved)
        
resolve_by_keeping_both('android/app/src/main/java/com/cimdriver/app/data/local/entity/Entities.kt')
resolve_by_keeping_both('android/app/src/main/java/com/cimdriver/app/data/local/dao/Daos.kt')
