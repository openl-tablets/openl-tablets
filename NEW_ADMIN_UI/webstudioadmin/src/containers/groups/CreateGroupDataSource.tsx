interface DataType {
  key: React.Key;
  privilege: string;
  administrators: string;
  analysts: string;
  deployers: string;
  developers: string;
  testers: string;
  viewers: string;
  [key: string]: string | React.Key;
}
