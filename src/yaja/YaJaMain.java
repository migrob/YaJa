package yaja;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

public class YaJaMain implements Listener, SelectionListener, BackSend {
  static final String sys_file_separator = System.getProperty("file.separator");
  static final String sys_user_dir = System.getProperty("user.dir");
  static String path_prop = /*sys_user_dir*/"." + sys_file_separator + "properties.conf";
  static final String title_param = "YaJa properties";
  final DkOperation dk_op = new DkOperation();
  final Prop_JSON prop_json = new Prop_JSON(path_prop);
  private int ind_dir_ti = -1;
  private Composite cmp_frm;
  private Button btn_sync;
  private Button btn_oauth;
  private Button btn_save;
  private Label lbl_dir_alias;
  private Text txt_dir_alias;
  private Label lbl_dir_loc;
  private Text txt_dir_loc;
  private Label lbl_dir_rem;
  private Text txt_dir_rem;
  private Table tbl_dirs;
  private OAuth oa_client = null;
  private Shell shell;
  private FormLayout lay_main;

  static String getMD5 (String file_path) throws NoSuchAlgorithmException, FileNotFoundException{
    FileInputStream is_fl = new FileInputStream(new File(file_path));
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] buffer = new byte[1024];
    int byte_read;
    try {
      while ((byte_read = is_fl.read(buffer)) > 0){
        md.update(buffer, 0, byte_read);
      }
    } catch(Exception ex){
      System.out.print(ex);
    } finally {
      try {
        is_fl.close();
      } catch (IOException ex) {}
    }
    StringBuffer sb_md = new StringBuffer();
    byte[] md_digest = md.digest();
    char[] hex_alph = new char[]{'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
    for (byte ch_dig: md_digest){
      sb_md.append(hex_alph[(ch_dig & 0xFF) >> 4]);
      sb_md.append(hex_alph[ch_dig & 0x0F]);
    }
    return sb_md.toString();
  }

  private static void DiskSync (DkOperation disk_op, File dir_loc, String path_rem)
    throws IOException, URISyntaxException, ParseException, NoSuchAlgorithmException
  {
    TreeSet<DkNode> list_rem = new TreeSet<DkNode>();
    File[] list_loc;
    list_loc = dir_loc.listFiles();
    if (list_loc == null) list_loc = new File[]{};
    Arrays.sort(
      list_loc
      , new Comparator(){
        @Override
        public int compare(Object o1, Object o2) {
          return ((File)o1).getName().compareTo(((File)o2).getName());
        }
      }
    );
    int list_limit = 20;
    int list_offset=0;
    int list_size;
    do {
      list_size = list_rem!=null ? list_rem.size() : 0;
      list_rem = (TreeSet<DkNode>) disk_op.GET_disk_resources_set(path_rem, list_offset, list_limit, list_rem);
      list_offset += list_limit;
    } while (!list_rem.isEmpty() && list_rem.size() != list_size);
    
    String href_dist = null;
    int i_loc = 0;
    Iterator it_rem = list_rem.iterator();
    DkNode rem_node = (it_rem.hasNext()?(DkNode)it_rem.next():null);
    int diff;
    while (i_loc < list_loc.length || rem_node != null) {
      if (rem_node==null) diff = -1;
      else if (i_loc >= list_loc.length) diff = 1;
      else diff = Integer.signum(list_loc[i_loc].getName().compareTo(rem_node.name));

      switch (diff) {
        case -1:
          if (list_loc[i_loc].isFile()) {
            href_dist = disk_op.GET_resources_upload_href(path_rem+"/"+list_loc[i_loc].getName(), false);
            disk_op.upload_file(href_dist, list_loc[i_loc]);
          } else {
            disk_op.PUT_disk_resources(path_rem+"/"+list_loc[i_loc].getName());
            DiskSync(disk_op, list_loc[i_loc], path_rem+"/"+list_loc[i_loc].getName());
            System.out.print("DIR ");
          }
          System.out.println("UPLOAD "+list_loc[i_loc].getName());
          i_loc++;
        break;
        case 0:
          if ((list_loc[i_loc].isFile()?DkNode.FILE:DkNode.DIR) == rem_node.type) {
            if (rem_node.type == DkNode.DIR) {
              DiskSync(disk_op, list_loc[i_loc], path_rem+"/"+rem_node.name);
            } else if (
              !getMD5(list_loc[i_loc].getCanonicalPath()).equals(rem_node.md5)
              || list_loc[i_loc].length() != rem_node.size
            ) {
              if (list_loc[i_loc].lastModified() < rem_node.modified.getTime()) {
                href_dist = disk_op.GET_resources_download_href(path_rem+"/"+rem_node.name);
                disk_op.download_file(href_dist, new File(dir_loc, rem_node.name), true);
                System.out.println("DOWNLOAD "+rem_node.name);
              } else if (list_loc[i_loc].lastModified() > rem_node.modified.getTime()) {
                href_dist = disk_op.GET_resources_upload_href(path_rem+"/"+list_loc[i_loc].getName(), true);
                disk_op.upload_file(href_dist, list_loc[i_loc]);
                System.out.println("UPLOAD "+list_loc[i_loc].getName());
              }
            }
          }
          i_loc++;
          rem_node = (it_rem.hasNext()?(DkNode)it_rem.next():null);
        break;
        case 1:
          if (rem_node.type == DkNode.FILE) {
            href_dist = disk_op.GET_resources_download_href(path_rem+"/"+rem_node.name);
            disk_op.download_file(href_dist, new File(dir_loc, rem_node.name), false);
          } else {
            File dir_loc_new = new File(dir_loc, rem_node.name);
            dir_loc_new.mkdir();
            DiskSync(disk_op, dir_loc_new, path_rem+"/"+rem_node.name);
            System.out.print("DIR ");
          }
          System.out.println("DOWNLOAD "+rem_node.name);
          rem_node = (it_rem.hasNext()?(DkNode)it_rem.next():null);
        break;
      }
    }
  }

  public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException {
    final YaJaMain main_frm = new YaJaMain();
    final Display display;
    final Shell shell;
    if (args.length > 0) {
      path_prop = args[0];
    }

    display = new Display();
    shell = new Shell(display);
    shell.setData(main_frm);
    main_frm.genForm(shell);

    try {
      main_frm.prop_json.loadJSON();
    } catch(Exception ex) {
      main_frm.prop_json.addDir();
      System.out.println(ex);
    }
    if (main_frm.prop_json.client_id == null) main_frm.prop_json.client_id = "5060b7bb172f4f4e9a514ce5a01ff888";
    if (main_frm.prop_json.client_secret == null) main_frm.prop_json.client_secret = "e618fcc0eb2f4fb4ac0b3a517137051b";
    
    shell.open();
    while (!shell.isDisposed()){
      if (!display.readAndDispatch())
        display.sleep();
    }
    display.dispose();
    main_frm.close();
  }

  public void genForm(Shell shell) {
    this.shell = shell;
    this.lay_main = new FormLayout();
    this.lay_main.marginHeight = 0;
    this.lay_main.marginWidth = 0;
    shell.setLayout(this.lay_main);
    FormData frm_dat = new FormData();
    frm_dat.left = new FormAttachment(0, 0);
    frm_dat.top = new FormAttachment(0, 0);
    frm_dat.right = new FormAttachment(100, 0);
    frm_dat.bottom = new FormAttachment(100, 0);
    cmp_frm = new Composite(shell, SWT.NONE);
    cmp_frm.setLayoutData(frm_dat);
    cmp_frm.setLayout(new FormLayout());
    btn_sync = new Button(cmp_frm, SWT.PUSH);
    btn_sync.setText("Sync");
    AttachControl(btn_sync, null, null, new FormAttachment(0, 0), null);
    btn_sync.addSelectionListener(this);

    lbl_dir_alias = new Label(cmp_frm, SWT.LEFT);
    lbl_dir_alias.setText("Метка");
    AttachControl(lbl_dir_alias, btn_sync, null, null, null);
    txt_dir_alias = new Text(cmp_frm, SWT.SINGLE);
    AttachControl(txt_dir_alias, lbl_dir_alias, null, null, null);
    txt_dir_alias.addListener(SWT.FocusOut, this);
    lbl_dir_loc = new Label(cmp_frm, SWT.LEFT);
    lbl_dir_loc.setText("Локальный каталог");
    AttachControl(lbl_dir_loc, txt_dir_alias, null, null, null);
    txt_dir_loc = new Text(cmp_frm, SWT.SINGLE);
    AttachControl(txt_dir_loc, lbl_dir_loc, null, null, null);
    txt_dir_loc.addListener(SWT.FocusOut, this);
    lbl_dir_rem = new Label(cmp_frm, SWT.LEFT);
    lbl_dir_rem.setText("Каталог ЯндексДиска");
    AttachControl(lbl_dir_rem, txt_dir_loc, null, null, null);
    txt_dir_rem = new Text(cmp_frm, SWT.SINGLE);
    AttachControl(txt_dir_rem, lbl_dir_rem, null, null, null);
    txt_dir_rem.addListener(SWT.FocusOut, this);
    btn_oauth = new Button(cmp_frm, SWT.PUSH);
    btn_oauth.setText("OAuth");
    AttachControl(btn_oauth, null, null, null, new FormAttachment(100, 0));
    btn_oauth.addSelectionListener(this);

    btn_save = new Button(cmp_frm, SWT.PUSH);
    btn_save.setText("Сохранить изменения");
    AttachControl(btn_save, txt_dir_rem, null, null, null);
    btn_save.addSelectionListener(this);
    tbl_dirs = prop_json.connectTable(cmp_frm, this);
    tbl_dirs.setLinesVisible(true);
    tbl_dirs.setHeaderVisible(true);
    AttachControl(tbl_dirs, btn_save, btn_oauth, null, null);

  }
  
  private void AttachControl(Control cntrl, Control ctl_top, Control ctl_bot, FormAttachment att_top, FormAttachment att_bot) {
    FormAttachment att_top_u = att_top!=null?att_top:(ctl_top!=null?new FormAttachment(ctl_top):null);
    FormAttachment att_bot_u = att_bot!=null?att_bot:(ctl_bot!=null?new FormAttachment(ctl_bot):null);
    FormData frm_dat = new FormData();
    frm_dat.left = new FormAttachment(0, 0);
    if (att_top_u != null) frm_dat.top = att_top_u;
    frm_dat.right = new FormAttachment(100, 0);
    if (att_bot_u != null) frm_dat.bottom = att_bot_u;
    frm_dat.height = cntrl.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
    cntrl.setLayoutData(frm_dat);
  }
  
  @Override
  public void widgetSelected(SelectionEvent se) {
    if (se.widget.equals(this.btn_sync)) {
      dk_op.setToken(this.prop_json.token);
      ArrayList<DkNode> nodes;
      try {
        for (int i=0; i<this.prop_json.getDirCount(); i++) {
          if (this.prop_json.getDirActive(i)) {
            File loc_dir_path = new File(this.prop_json.getDirLoc(i));
            DiskSync(dk_op, loc_dir_path, this.prop_json.getDirRem(i));
          }
        }
        ParamCommit();
      } catch (Exception ex) {
        System.out.print(ex);
        for (StackTraceElement el_tr: ex.getStackTrace())
          System.out.println(el_tr.toString());
      }
    } else if (se.widget.equals(this.btn_oauth)){
      System.out.println("Button OAuth: " + String.valueOf(Thread.currentThread().getId()));
      cmp_frm.setVisible(false);
      shell.addListener(SWT.Activate, this);
      oa_client = new OAuth(
        this.shell
        , this.prop_json.client_id
        , this.prop_json.client_secret
      );
      shell.layout();
    }
  }

  @Override
  public void widgetDefaultSelected(SelectionEvent se) {}
  
  @Override
  public void handleEvent(Event event) {
    final String str_exc = "exception";
    if (
      event.widget.equals(this.shell)
      && event.type == SWT.Activate
      && event.text != null
    ){
      cmp_frm.setVisible(true);
      if (!event.text.substring(0, str_exc.length()).equals(str_exc)) {
        this.prop_json.token = event.text;
        ParamCommit();
      }
      shell.removeListener(SWT.Activate, this);
      shell.layout();
      System.out.println("handleEvent: " + String.valueOf(Thread.currentThread().getId()));
      oa_client.dispose();
    } else if (
      event.type == SWT.FocusOut
      && this.ind_dir_ti >= 0
    ){
      if (event.widget.equals(this.txt_dir_alias))
        this.prop_json.setDirAlias(this.ind_dir_ti, this.txt_dir_alias.getText());
      else if (event.widget.equals(this.txt_dir_loc))
        this.prop_json.setDirLoc(this.ind_dir_ti, this.txt_dir_loc.getText());
      else if (event.widget.equals(this.txt_dir_rem))
        this.prop_json.setDirRem(this.ind_dir_ti, this.txt_dir_rem.getText());
    }
  }

  private void ParamCommit(){
    try {
      prop_json.saveJSON();
    } catch (IOException ex) {
      System.out.print(ex);
    }
  }
  
  public void close() {
    try {
      this.dk_op.close();
    } catch (IOException ex) {
      System.out.print(ex);
    }
  }

  @Override
  public void setDirInfo(int ind_ti) {
    ind_dir_ti = ind_ti;
    txt_dir_alias.setText(prop_json.getDirAlias(ind_ti));
    txt_dir_loc.setText(prop_json.getDirLoc(ind_ti));
    txt_dir_rem.setText(prop_json.getDirRem(ind_ti));
  }

}
