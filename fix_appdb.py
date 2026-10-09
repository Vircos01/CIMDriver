import re

with open('android/app/src/main/java/com/cimdriver/app/data/local/AppDatabase.kt', 'r') as f:
    content = f.read()

# 1. Entities and Version
content = re.sub(
    r'<<<<<<< HEAD\n\s*Client::class,\n\s*ProjectCode::class,\n\s*HoursTarget::class\n\s*],\ \n\s*version\ =\ 37,\ \n=======\n\s*FavoriteRoute::class,\n\s*OdometerCheck::class,\n\s*FuelFillUp::class\n\s*],\ \n\s*version\ =\ 41,\ \n>>>>>>> origin/main',
    '''        Client::class,
        ProjectCode::class,
        HoursTarget::class,
        FavoriteRoute::class,
        OdometerCheck::class,
        FuelFillUp::class
    ], 
    version = 42, ''', content)

# 2. DAOs
content = re.sub(
    r'<<<<<<< HEAD\n\s*abstract fun clientDao\(\): ClientDao\n\s*abstract fun projectCodeDao\(\): ProjectCodeDao\n\s*abstract fun hoursTargetDao\(\): HoursTargetDao\n=======\n\s*abstract fun favoriteRouteDao\(\): FavoriteRouteDao\n\s*abstract fun odometerCheckDao\(\): OdometerCheckDao\n\s*abstract fun fuelFillUpDao\(\): FuelFillUpDao\n>>>>>>> origin/main',
    '''	abstract fun clientDao(): ClientDao
	abstract fun projectCodeDao(): ProjectCodeDao
	abstract fun hoursTargetDao(): HoursTargetDao
	abstract fun favoriteRouteDao(): FavoriteRouteDao
	abstract fun odometerCheckDao(): OdometerCheckDao
	abstract fun fuelFillUpDao(): FuelFillUpDao''', content)

# 3. MIGRATION_35_36
# In HEAD, MIGRATION_35_36 adds engineType and fuel_entries. In main it adds inServiceDate.
# We keep main's version, because main is truth.
# But wait, python regex for this block is long.
# Let's find the exact text
pattern_35_36 = r'<<<<<<< HEAD.*?hasEngineType = false.*?if \(\!fuelTableExists\).*?=======.*?inServiceDate.*?>>>>>>> origin/main'
content = re.sub(pattern_35_36, 
    '''                db.execSQL("ALTER TABLE vehicles ADD COLUMN inServiceDate INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN endServiceDate INTEGER DEFAULT NULL")''', content, flags=re.DOTALL)

# 4. MIGRATION_36_37 and upwards
# The HEAD has MIGRATION_36_37 which creates clients, project_codes, hours_targets
# The main has MIGRATION_36_37 up to 40_41.
# We want to change HEAD's 36_37 to 41_42, and keep main's 36_37...40_41.
def replacer_migrations(m):
    head_code = m.group(1).replace('val MIGRATION_36_37 = object : Migration(36, 37)', 'val MIGRATION_41_42 = object : Migration(41, 42)')
    main_code = m.group(2)
    return main_code + "\n\n" + head_code

content = re.sub(r'<<<<<<< HEAD\n(.*?)=======\n(.*?)>>>>>>> origin/main', replacer_migrations, content, flags=re.DOTALL)

# 5. getDatabase
# Instead of complex regex, let's just replace the build method.
content = re.sub(r'<<<<<<< HEAD.*?\.addCallback.*?=======(.*?)\.addCallback.*?>>>>>>> origin/main', 
                 r'\1, MIGRATION_41_42).addCallback', content, flags=re.DOTALL)

with open('android/app/src/main/java/com/cimdriver/app/data/local/AppDatabase.kt', 'w') as f:
    f.write(content)
