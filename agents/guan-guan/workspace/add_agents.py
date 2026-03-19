import json

with open(r'C:\Users\16343\.openclaw\openclaw.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

new_agents = [
    {
        'id': 'chan-pin-jing-li',
        'name': '产品经理',
        'workspace': 'C:\\Users\\16343\\.openclaw\\agents\\chan-pin-jing-li\\workspace',
        'agentDir': 'C:\\Users\\16343\\.openclaw\\agents\\chan-pin-jing-li',
        'identity': {'name': '产品经理', 'theme': 'professional', 'emoji': '📋'}
    },
    {
        'id': 'xiang-mu-jing-li',
        'name': '项目经理',
        'workspace': 'C:\\Users\\16343\\.openclaw\\agents\\xiang-mu-jing-li\\workspace',
        'agentDir': 'C:\\Users\\16343\\.openclaw\\agents\\xiang-mu-jing-li',
        'identity': {'name': '项目经理', 'theme': 'professional', 'emoji': '📊'}
    },
    {
        'id': 'qian-duan-kai-fa',
        'name': '前端开发',
        'workspace': 'C:\\Users\\16343\\.openclaw\\agents\\qian-duan-kai-fa\\workspace',
        'agentDir': 'C:\\Users\\16343\\.openclaw\\agents\\qian-duan-kai-fa',
        'identity': {'name': '前端开发', 'theme': 'creative', 'emoji': '💻'}
    }
]

data['agents']['list'].extend(new_agents)

with open(r'C:\Users\16343\.openclaw\openclaw.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print('Agents added successfully!')
