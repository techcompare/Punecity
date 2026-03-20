import json

FILE = r"C:\Users\prana\AndroidStudioProjects\Punecity\app\src\main\assets\attractions.json"

# New fields keyed by attraction name
new_fields = {
    "Viman Nagar Social": {"neighborhood": "Viman Nagar", "budgetFriendly": False, "hasWifi": True, "tags": "student,group,evening,night,weekend,food,wifi"},
    "Fergusson College Road (FC Road)": {"neighborhood": "Deccan", "budgetFriendly": True, "hasWifi": False, "tags": "student,instagrammable,group,evening,food,free-entry"},
    "Vaishali Restaurant": {"neighborhood": "Deccan", "budgetFriendly": True, "hasWifi": False, "tags": "student,group,morning,evening,food"},
    "Vetal Tekdi": {"neighborhood": "Pashan", "budgetFriendly": True, "hasWifi": False, "tags": "student,quiet,solo,group,morning,evening,weekend,monsoon,winter,free-entry,trek,nature"},
    "Goodluck Cafe": {"neighborhood": "Deccan", "budgetFriendly": True, "hasWifi": False, "tags": "student,solo,group,morning,food,history"},
    "SPPU (Pune University)": {"neighborhood": "Aundh", "budgetFriendly": True, "hasWifi": True, "tags": "student,quiet,instagrammable,solo,morning,evening,winter,free-entry,nature,study,wifi"},
    "Okayama Friendship Garden": {"neighborhood": "Sinhagad Road", "budgetFriendly": True, "hasWifi": False, "tags": "instagrammable,family,solo,morning,evening,winter,nature"},
    "Durvankur Thali": {"neighborhood": "Sadashiv Peth", "budgetFriendly": False, "hasWifi": False, "tags": "family,group,food"},
    "Aga Khan Palace": {"neighborhood": "Kalyani Nagar", "budgetFriendly": True, "hasWifi": False, "tags": "student,quiet,instagrammable,family,solo,morning,winter,history"},
    "Shaniwar Wada": {"neighborhood": "Shaniwar Peth", "budgetFriendly": True, "hasWifi": False, "tags": "student,instagrammable,family,evening,weekend,history"},
    "Dagdusheth Ganpati": {"neighborhood": "Budhwar Peth", "budgetFriendly": True, "hasWifi": False, "tags": "family,solo,group,morning,evening,weekend,free-entry,spiritual"},
    "Sinhagad Fort": {"neighborhood": "Sinhagad", "budgetFriendly": True, "hasWifi": False, "tags": "student,instagrammable,group,morning,weekend,monsoon,winter,trek,food,history,nature,adventure"},
    "Saras Baug": {"neighborhood": "Swargate", "budgetFriendly": True, "hasWifi": False, "tags": "family,solo,evening,weekend,free-entry,spiritual,nature"},
    "Katraj Snake Park": {"neighborhood": "Katraj", "budgetFriendly": True, "hasWifi": False, "tags": "family,group,morning,weekend,winter,nature"},
    "Sujata Mastani": {"neighborhood": "Sadashiv Peth", "budgetFriendly": True, "hasWifi": False, "tags": "student,instagrammable,family,solo,group,evening,food"},
    "Raja Dinkar Kelkar Museum": {"neighborhood": "Shaniwar Peth", "budgetFriendly": True, "hasWifi": False, "tags": "student,quiet,family,solo,morning,weekend,history"},
    "Parvati Hill": {"neighborhood": "Parvati", "budgetFriendly": True, "hasWifi": False, "tags": "student,instagrammable,solo,group,morning,weekend,winter,free-entry,trek,spiritual,nature"},
    "Lal Mahal": {"neighborhood": "Kasba Peth", "budgetFriendly": True, "hasWifi": False, "tags": "student,family,morning,history"},
    "Osho International Meditation Resort": {"neighborhood": "Koregaon Park", "budgetFriendly": False, "hasWifi": False, "tags": "quiet,solo,morning,spiritual"},
    "Pashan Lake": {"neighborhood": "Pashan", "budgetFriendly": True, "hasWifi": False, "tags": "quiet,solo,morning,winter,free-entry,nature"},
    "Kirkee War Cemetery": {"neighborhood": "Khadki", "budgetFriendly": True, "hasWifi": False, "tags": "quiet,solo,morning,free-entry,history"},
    "Koregaon Park": {"neighborhood": "Koregaon Park", "budgetFriendly": True, "hasWifi": True, "tags": "instagrammable,date-night,solo,group,evening,weekend,food,wifi"},
    "Chaturshringi Temple": {"neighborhood": "Shivajinagar", "budgetFriendly": True, "hasWifi": False, "tags": "family,solo,group,morning,evening,free-entry,spiritual"},
    "Empress Botanical Garden": {"neighborhood": "Camp", "budgetFriendly": True, "hasWifi": False, "tags": "family,solo,morning,winter,nature"},
    "Khadakwasla Dam": {"neighborhood": "Khadakwasla", "budgetFriendly": True, "hasWifi": False, "tags": "instagrammable,family,group,evening,weekend,monsoon,free-entry,nature"},
    "Baner-Balewadi Promenade": {"neighborhood": "Baner", "budgetFriendly": True, "hasWifi": True, "tags": "instagrammable,date-night,group,evening,night,weekend,food,wifi"},
    "Taljai Tekdi": {"neighborhood": "Bibwewadi", "budgetFriendly": True, "hasWifi": False, "tags": "quiet,solo,morning,free-entry,trek,nature"},
    "Phursungi Mula-Mutha Riverside": {"neighborhood": "Hadapsar", "budgetFriendly": True, "hasWifi": False, "tags": "quiet,solo,morning,winter,free-entry,nature"},
    "Pune-Okayama Friendship Road": {"neighborhood": "Sinhagad Road", "budgetFriendly": True, "hasWifi": False, "tags": "quiet,instagrammable,solo,morning,free-entry,nature"},
    "Tribal Museum (Adivasiyan Sangrahalay)": {"neighborhood": "Koregaon Park", "budgetFriendly": True, "hasWifi": False, "tags": "student,quiet,solo,morning,history"},
    "Katraj Ghat": {"neighborhood": "Katraj", "budgetFriendly": True, "hasWifi": False, "tags": "instagrammable,group,morning,weekend,monsoon,free-entry,nature,adventure"},
    "Camp (MG Road Area)": {"neighborhood": "Camp", "budgetFriendly": True, "hasWifi": False, "tags": "instagrammable,solo,group,evening,weekend,food,history,free-entry"},
    "Kayani Bakery": {"neighborhood": "Camp", "budgetFriendly": True, "hasWifi": False, "tags": "student,family,solo,group,morning,food"},
    "Chhatrapati Shivaji Maharaj Museum (Prince of Wales Museum replica)": {"neighborhood": "Camp", "budgetFriendly": True, "hasWifi": False, "tags": "student,quiet,family,solo,morning,weekend,history"},
    "Bund Garden": {"neighborhood": "Koregaon Park", "budgetFriendly": True, "hasWifi": False, "tags": "family,solo,evening,weekend,nature"},
    "Bhimashankar Temple": {"neighborhood": "Bhimashankar", "budgetFriendly": True, "hasWifi": False, "tags": "group,morning,weekend,monsoon,free-entry,trek,spiritual,nature,adventure"},
    "Pataleshwar Cave Temple": {"neighborhood": "Shivajinagar", "budgetFriendly": True, "hasWifi": False, "tags": "student,quiet,solo,morning,free-entry,history,spiritual"},
    "Katraj Lake": {"neighborhood": "Katraj", "budgetFriendly": True, "hasWifi": False, "tags": "quiet,family,solo,morning,evening,nature"},
    "Misal Parade (Bedekar Misal)": {"neighborhood": "Narayan Peth", "budgetFriendly": True, "hasWifi": False, "tags": "student,solo,group,morning,food"},
    "Vishrambaug Wada": {"neighborhood": "Sadashiv Peth", "budgetFriendly": True, "hasWifi": False, "tags": "student,quiet,solo,morning,free-entry,history"},
    "Mulshi Lake": {"neighborhood": "Mulshi", "budgetFriendly": True, "hasWifi": False, "tags": "instagrammable,group,weekend,monsoon,free-entry,nature,adventure"},
    "Rajmachi Fort": {"neighborhood": "Lonavala", "budgetFriendly": True, "hasWifi": False, "tags": "group,weekend,monsoon,free-entry,trek,history,nature,adventure"},
    "Hadapsar IT Park (Magarpatta City)": {"neighborhood": "Hadapsar", "budgetFriendly": True, "hasWifi": True, "tags": "instagrammable,solo,group,morning,weekend,free-entry,wifi"},
    "Aga Khan Palace Garden": {"neighborhood": "Kalyani Nagar", "budgetFriendly": True, "hasWifi": False, "tags": "quiet,instagrammable,family,solo,morning,winter,nature,history"},
}

# Read file
with open(FILE, "r", encoding="utf-8") as f:
    raw = f.read()

# Find first complete array by matching the first ']'
bracket_count = 0
end_idx = None
for i, ch in enumerate(raw):
    if ch == '[':
        bracket_count += 1
    elif ch == ']':
        bracket_count -= 1
        if bracket_count == 0:
            end_idx = i + 1
            break

valid_json = raw[:end_idx]
data = json.loads(valid_json)

print(f"Parsed {len(data)} attractions")

# Verify all names match
for item in data:
    name = item["name"]
    if name not in new_fields:
        print(f"WARNING: No new fields for '{name}'")

# Add new fields to each object
for item in data:
    name = item["name"]
    if name in new_fields:
        fields = new_fields[name]
        item["neighborhood"] = fields["neighborhood"]
        item["budgetFriendly"] = fields["budgetFriendly"]
        item["hasWifi"] = fields["hasWifi"]
        item["tags"] = fields["tags"]

# Write back
with open(FILE, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)
    f.write("\n")

print(f"Written {len(data)} attractions with new fields")

# Verify
with open(FILE, "r", encoding="utf-8") as f:
    verify = json.load(f)
print(f"Verification: {len(verify)} objects")
for item in verify:
    assert "neighborhood" in item, f"Missing neighborhood in {item['name']}"
    assert "budgetFriendly" in item, f"Missing budgetFriendly in {item['name']}"
    assert "hasWifi" in item, f"Missing hasWifi in {item['name']}"
    assert "tags" in item, f"Missing tags in {item['name']}"
print("All 4 new fields present in all objects. Done!")
