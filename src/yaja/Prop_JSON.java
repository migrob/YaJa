package yaja;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class Prop_JSON implements SelectionListener{
  private final String file_path;
  private Table tbl_dirs = null;
  private BackSend bs_editor = null;
  String client_id = null;
  String client_secret = null;
  String token = null;
  private JSONParser json_parser = null;
  Prop_JSON (String JSON_file) {
    file_path = JSON_file;
  }
  public Table connectTable(Composite par, BackSend bs_editor) {
    this.bs_editor = bs_editor;
    tbl_dirs = new Table(par, SWT.CHECK);
    TableColumn tc;
    tc = new TableColumn(tbl_dirs, SWT.LEFT);
    tc.setText("метка");
    tc = new TableColumn(tbl_dirs, SWT.LEFT);
    tc.setText("локальный");
    tc = new TableColumn(tbl_dirs, SWT.LEFT);
    tc.setText("yandex");
    
    tbl_dirs.addSelectionListener(this);

    return tbl_dirs;
  }
  public void setDirActive(int it_ind, boolean bl_arg){
    tbl_dirs.getItem(it_ind).setChecked(bl_arg);
  }
  public void setDirAlias(int it_ind, String st_arg){
    tbl_dirs.getItem(it_ind).setText(0, st_arg);
  }
  public void setDirLoc(int it_ind, String st_arg){
    tbl_dirs.getItem(it_ind).setText(1, st_arg);
  }
  public void setDirRem(int it_ind, String st_arg){
    tbl_dirs.getItem(it_ind).setText(2, st_arg);
  }
  public boolean getDirActive(int it_ind){
    return tbl_dirs.getItem(it_ind).getChecked();
  }
  public String getDirAlias(int it_ind){
    return tbl_dirs.getItem(it_ind).getText(0);
  }
  public String getDirLoc(int it_ind){
    return tbl_dirs.getItem(it_ind).getText(1);
  }
  public String getDirRem(int it_ind){
    return tbl_dirs.getItem(it_ind).getText(2);
  }
  public int getDirCount(){
    return tbl_dirs.getItemCount();
  }
  public void addDir(){
    new TableItem(tbl_dirs, SWT.NONE);
  }
  public void saveJSON() throws FileNotFoundException, UnsupportedEncodingException{
    JsonGenerator json_gen = Json.createGenerator(new OutputStreamWriter(new FileOutputStream(file_path), "UTF8"));
    json_gen.writeStartObject()
      .write("client_id", client_id != null ? client_id : "")
      .write("client_secret", client_secret != null ? client_secret : "")
      .write("token", token != null ? token : "")
      .writeStartArray("dirs");
    for (int i=0; i<tbl_dirs.getItemCount(); i++) {
      json_gen.writeStartObject()
        .write("active", tbl_dirs.getItem(i).getChecked())
        .write("alias", getDirAlias(i))
        .write("dir_loc", getDirLoc(i))
        .write("dir_rem", getDirRem(i))
      .writeEnd();
    }
    json_gen.writeEnd().writeEnd();
    json_gen.close();
  }
  @SuppressWarnings("empty-statement")
  public void loadJSON() throws UnsupportedEncodingException, FileNotFoundException{
    json_parser = new JSONParser(new InputStreamReader(new FileInputStream(file_path), "UTF8"));
    tbl_dirs.removeAll();
    int dir_num = -1;
    TableItem ti_cur = null;
    for (StringBuffer path = null; json_parser.hasNext(); path = json_parser.NextKey()) {
      System.out.println(path == null ? "" : path.toString());
      if (json_parser.path_equal("{client_id")) {
        client_id = json_parser.getValueString();
      } else if (json_parser.path_equal("{client_secret")) {
        client_secret = json_parser.getValueString();
      } else if (json_parser.path_equal("{token")) {
        token = json_parser.getValueString();
      } else if (json_parser.path_equal("{dirs[{")) {
        ti_cur = new TableItem(tbl_dirs, SWT.NONE);
        dir_num++;
      } else if (json_parser.path_equal("{dirs[{active")) {
        ti_cur.setChecked(json_parser.getValueBoolean());
      } else if (json_parser.path_equal("{dirs[{alias")) {
        setDirAlias(dir_num, json_parser.getValueString());
      } else if (json_parser.path_equal("{dirs[{dir_loc")) {
        setDirLoc(dir_num, json_parser.getValueString());
      } else if (json_parser.path_equal("{dirs[{dir_rem")) {
        setDirRem(dir_num, json_parser.getValueString());
      }
    }
    json_parser.close();
    for (int i=0; i<tbl_dirs.getColumnCount(); i++)
      tbl_dirs.getColumn(i).pack();
  }

  @Override
  public void widgetSelected(SelectionEvent se) {
    if (bs_editor != null && se.item != null) bs_editor.setDirInfo(this.tbl_dirs.indexOf((TableItem)se.item));
  }

  @Override
  public void widgetDefaultSelected(SelectionEvent se) {}

}
