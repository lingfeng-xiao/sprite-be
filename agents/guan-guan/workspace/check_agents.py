import json

with open(r'C:\Users\16343\.openclaw\openclaw.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

print("Current agents:")
for agent in data['agents']['list']:
    print(f"  - {agent['id']}: {agent.get('name', 'N/A')}")
