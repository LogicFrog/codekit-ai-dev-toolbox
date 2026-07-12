const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

class FileHashIndex {
  constructor(rootDir) {
    this.rootDir = rootDir;
    this.index = new Map(); // filePath -> { md5, size, mtime }
  }

  scan() {
    this._walk(this.rootDir);
    return this.index.size;
  }

  _walk(dir) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory() && !entry.name.startsWith('.')) {
        this._walk(fullPath);
      } else if (entry.isFile()) {
        this._indexFile(fullPath);
      }
    }
  }

  _indexFile(filePath) {
    const content = fs.readFileSync(filePath);
    const md5 = crypto.createHash('md5').update(content).digest('hex');
    const stat = fs.statSync(filePath);
    this.index.set(filePath, {
      md5,
      size: stat.size,
      mtime: stat.mtime.toISOString(),
    });
  }

  findDuplicates() {
    const hashGroups = new Map();
    for (const [filePath, meta] of this.index) {
      const list = hashGroups.get(meta.md5) || [];
      list.push(filePath);
      hashGroups.set(meta.md5, list);
    }
    return [...hashGroups.values()].filter(group => group.length > 1);
  }
}

module.exports = FileHashIndex;
