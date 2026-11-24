---
table: Test greet greetTest
---

> A \u not followed by four hexadecimal digits breaks only its own text, which is read as written: the other texts
> of the language and the other languages are read as usual.

| _context_.locale | key      | name | _res_                |
|------------------|----------|------|----------------------|
| Locale           | Key      | Name | Text                 |
| it               | greeting | Ann  | Ciao, Ann!           |
| it               | broken   |      | \u12G4 broken escape |
| it               | path     |      | C:\users             |
| it               | farewell |      | Goodbye              |
| de               | greeting | Ann  | Hallo, Ann!          |
|                  | greeting | Ann  | Hello, Ann!          |
