package yaja;

public class SyncDir {
  boolean active = false;
  String alias;
  String loc_dir;
  String rem_dir;
  SyncDir() {}
  SyncDir(boolean ac, String al, String ld, String rd) {
    this.set(ac, al, ld, rd);
  }
  final void set(boolean ac, String al, String ld, String rd){
    active = ac;
    alias = al;
    loc_dir = ld;
    rem_dir = rd;
  }
}
