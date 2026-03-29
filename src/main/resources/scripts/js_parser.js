const esprima = require('esprima');
const fs = require('fs');

function parseJS(code) {
    try {
        let ast;
        try {
            ast = esprima.parseModule(code, { jsx: true, tolerant: true, range: true, tokens: true });
        } catch (e) {
            ast = esprima.parseScript(code, { jsx: true, tolerant: true, range: true, tokens: true });
        }
        
        const result = {
            className: null,
            functions: [],
            imports: []
        };

        function traverse(node) {
            if (!node) return;

            // 类定义
            if (node.type === 'ClassDeclaration' || node.type === 'ClassExpression') {
                if (node.id && node.id.name) {
                    if (result.className === null) {
                        result.className = node.id.name;
                    } else {
                        result.functions.push(`innerClass:${node.id.name}`);
                    }
                }
            } 
            // 函数定义
            else if (node.type === 'FunctionDeclaration') {
                if (node.id && node.id.name) {
                    result.functions.push(node.id.name);
                }
            } 
            // 变量赋值中的匿名函数/箭头函数
            else if (node.type === 'VariableDeclarator') {
                if (node.init && (node.init.type === 'FunctionExpression' || node.init.type === 'ArrowFunctionExpression')) {
                    if (node.id && node.id.name) {
                        result.functions.push(node.id.name);
                    }
                }
            }
            // 类的方法
            else if (node.type === 'MethodDefinition') {
                if (node.key && node.key.name) {
                    result.functions.push(`method:${node.key.name}`);
                }
            }
            // ES6 导入
            else if (node.type === 'ImportDeclaration') {
                if (node.source && node.source.value) {
                    result.imports.push(node.source.value);
                }
            } 
            // CommonJS require
            else if (node.type === 'CallExpression' && node.callee.name === 'require') {
                if (node.arguments.length > 0 && node.arguments[0].type === 'Literal') {
                    result.imports.push(node.arguments[0].value);
                }
            }

            for (let key in node) {
                if (node[key] && typeof node[key] === 'object') {
                    if (Array.isArray(node[key])) {
                        node[key].forEach(child => traverse(child));
                    } else {
                        traverse(node[key]);
                    }
                }
            }
        }

        traverse(ast);
        return result;
    } catch (e) {
        return { error: e.message };
    }
}

try {
    const code = fs.readFileSync(0, 'utf-8');
    console.log(JSON.stringify(parseJS(code)));
} catch (e) {
    console.log(JSON.stringify({ error: e.message }));
}
