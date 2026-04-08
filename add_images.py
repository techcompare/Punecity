import json

# Read the current data
with open('data.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

# Add image URLs to each entry
for item in data:
    # Create a slug from the name for the image filename
    slug = item['name'].lower()
    slug = slug.replace(' ', '-')
    slug = slug.replace("'", '')
    slug = slug.replace('.', '')
    # Add the image URL
    item['image'] = f"https://raw.githubusercontent.com/techcompare/Punecity/main/images/{slug}.jpg"

# Write back the updated data
with open('data.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=2, ensure_ascii=False)

print(f"✓ Added image URLs to {len(data)} attractions")
