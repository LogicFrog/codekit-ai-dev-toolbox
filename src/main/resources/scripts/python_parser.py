import sys
import json
import astroid

def parse_python(code):
    try:
        module = astroid.parse(code)
        result = {
            "className": None,
            "functions": [],
            "imports": []
        }
        
        # 遍历顶层节点
        for node in module.body:
            if isinstance(node, astroid.ClassDef):
                if result["className"] is None:
                    result["className"] = node.name
                else:
                    result["functions"].append(f"innerClass:{node.name}")
                
                # 类内部的方法
                for subnode in node.body:
                    if isinstance(subnode, astroid.FunctionDef):
                        if not subnode.name.startswith("__"):
                            result["functions"].append(f"method:{subnode.name}")
            
            elif isinstance(node, astroid.FunctionDef):
                if not node.name.startswith("__"):
                    result["functions"].append(node.name)
        
        # 提取导入
        for node in module.nodes_of_class((astroid.Import, astroid.ImportFrom)):
            if isinstance(node, astroid.Import):
                for name, _ in node.names:
                    result["imports"].append(name)
            elif isinstance(node, astroid.ImportFrom):
                result["imports"].append(node.modname)
                
        return result
    except Exception as e:
        return {"error": str(e)}

if __name__ == "__main__":
    try:
        code = sys.stdin.read()
        print(json.dumps(parse_python(code)))
    except Exception as e:
        print(json.dumps({"error": str(e)}))
