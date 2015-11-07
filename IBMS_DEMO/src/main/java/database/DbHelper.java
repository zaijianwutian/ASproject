package database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
	private final static String DATABASE_NAME = "BOOKS.db";
	private final static int DATABASE_VERSION = 1;
	private final static String TABLE_NAME = "books_table";
	public final static String BOOK_ID = "book_id";
	public final static String BOOK_NAME = "book_name";
	public final static String BOOK_AUTHOR = "book_author";
	public DbHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub      INTEGER primary key autoincrement自增长
		//String sql = "drop table if exists switch_tb " ;
	    String sql = " create table  switchs_tb ( CID INTEGER primary key autoincrement,"  //通道ID 唯一标示通道 ，自增长
				+ "State TEXT,"   //开关状态   "开"或“关”
				+ "IPAddr TEXT,"   //开关IP地址  
				+ "MACAddr TEXT,"  //开关的MAC地址
				+ "RSAddr TEXT,"     //开关的485地址，4个长度的字符串，如0000,0001
				+ "Type TEXT,"      //类型，如“插座”等
				+ "Channel TEXT,"  //某个开关的Channel号  从0到10   0代表总开关
				+ "IValue TEXT,"   //电流值
				+ "Area TEXT,"     //该通道所在的区域，厨房、卧室、客厅等
				+ "Name TEXT,"     //通道的别名，如客厅灯，插座。。等
				+ "GetTime TEXT,"  //最后一次刷新的时间		
				+ "Image BLOB);";    //开关图标
		db.execSQL(sql);     //创建开关表
		
		String sql2 = " create table  users_tb ( NID INTEGER primary key autoincrement,"  //用户名ID 唯一标示通道 ，自增长
				+ "Name TEXT,"   //用户姓名
				+ "Password TEXT,"//用户密码 
				+ "RSAddr TEXT,"     //开关的485地址，4个长度的字符串，如0000,0001
				+ "IsUsing TEXT,"//是否正在使用,1表示正在使用，0表示没有在使用 
				+ "Auto TEXT,"   //是否自动登录，1表示自动登录，0表示不自动登录 
				+ "Remember TEXT);" ;  //是否记住密码，1表示记住密码，0表示不记住密码 
		db.execSQL(sql2);    //创建用户表
		
		String sql3 = " create table  room_tb ( RID INTEGER primary key autoincrement,"  //房间ID 唯一标示通道 ，自增长
				+ "Name TEXT,"     //房间名称
				+ "Image BLOB);";    //房间图标
		db.execSQL(sql3);    //创建房间表
		
		String sql4 = " create table  scene_tb ( SID INTEGER primary key autoincrement,"  //房间ID 唯一标示通道 ，自增长
				+ "Name TEXT,"     //场景名称
				+ "Image BLOB,"    //场景图标
				+ "Operation TEXT," //操作，1代表打开，0代表关闭
				+ "CID TEXT,"       //对应的通道ID
				+ "Delay TEXT);";    //延时操作时间，单位是秒
		db.execSQL(sql4);    //创建场景命令表
		
		String sql5 = " create table  scenename_tb ( SID INTEGER primary key autoincrement,"  //房间ID 唯一标示通道 ，自增长
				+ "Name TEXT,"     //场景名称
				+ "Image BLOB);" ; //场景图标
				
		db.execSQL(sql5);    //创建场景命令表
		
		
	}

	@Override    //数据库版本更新的时候调用
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}
	//查询操作
	public Cursor select() {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query("switch_tb", null, null, null, null, null, null);
		return cursor;
	}
	//增加操作
	public long insert(String bookname,String author)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		/* ContentValues */
		ContentValues cv = new ContentValues();
		cv.put(BOOK_NAME, bookname);
		cv.put(BOOK_AUTHOR, author);
		long row = db.insert(TABLE_NAME, null, cv);
		
		
		return row;
	}
	//删除操作
	public void delete(int id)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		String where = BOOK_ID + " = ?";
		String[] whereValue ={ Integer.toString(id) };
		db.delete(TABLE_NAME, where, whereValue);
	}
	//修改操作
	public void update(int id, String bookname,String author)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		String where = BOOK_ID + " = ?";
		String[] whereValue = { Integer.toString(id) };
		 
		ContentValues cv = new ContentValues();
		cv.put(BOOK_NAME, bookname);
		cv.put(BOOK_AUTHOR, author);
		db.update(TABLE_NAME, cv, where, whereValue);
	}
}
