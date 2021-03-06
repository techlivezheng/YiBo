package net.dev123.yibo.service.task;

import java.util.ArrayList;
import java.util.List;

import net.dev123.commons.util.ListUtil;
import net.dev123.entity.BaseUser;
import net.dev123.exception.LibException;
import net.dev123.mblog.MicroBlog;
import net.dev123.yibo.R;
import net.dev123.yibo.common.Constants;
import net.dev123.yibo.common.GlobalVars;
import net.dev123.yibo.common.ResourceBook;
import net.dev123.yibo.db.LocalAccount;
import net.dev123.yibo.db.UserGroupDao;
import net.dev123.yibo.service.adapter.GroupMemberListAdapter;
import net.dev123.yibome.entity.LocalGroup;
import net.dev123.yibome.entity.UserGroup;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

public class GroupMemberDeleteTask extends AsyncTask<Void, BaseUser, List<BaseUser>> {
	private static final String TAG = "GroupMemberAddTask";

	private Context context;
    private GroupMemberListAdapter adapter;
    private LocalGroup group;
    private List<BaseUser> userList;

    private LocalAccount account;
    private MicroBlog microBlog;

    private ProgressDialog dialog;
    private Button btnDelete;
    private String resultMsg;
	public GroupMemberDeleteTask(GroupMemberListAdapter adapter, LocalGroup group, List<BaseUser> userList) {
		this.adapter = adapter;
		this.group = group;
		this.userList = userList;
		this.context = adapter.getContext();
		this.account = adapter.getAccount();
		this.microBlog = GlobalVars.getMicroBlog(account);
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		btnDelete = (Button)((Activity)context).findViewById(R.id.btnDelete);
		btnDelete.setEnabled(false);

		dialog = ProgressDialog.show(context, null,
			context.getString(R.string.msg_group_member_delete));
	    dialog.setCancelable(true);
	    dialog.setOnCancelListener(onCancelListener);
	    dialog.setOwnerActivity((Activity)context);
	}

	@Override
	protected List<BaseUser> doInBackground(Void... params) {
		if (ListUtil.isEmpty(userList) || microBlog == null) {
			return null;
		}

		List<BaseUser> deletedUserList = new ArrayList<BaseUser>();
		for (BaseUser user : userList) {
			try {
		        microBlog.destroyGroupMember(group.getSpGroupId(), user.getId());
		        deletedUserList.add(user);
		    } catch (LibException e) {
			    if (Constants.DEBUG) Log.e(TAG, "Task", e);
			    resultMsg = ResourceBook.getStatusCodeValue(e.getExceptionCode(), context);
			    this.publishProgress(user);
		    }
		}

		return deletedUserList;
	}

	@Override
	protected void onProgressUpdate(BaseUser... values) {
		super.onProgressUpdate(values);
		BaseUser user = values[0];
		if (user == null) {
			return;
		}

		String msg = context.getString(R.string.msg_group_member_delete_failed,
			user.getDisplayName(), resultMsg);
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onPostExecute(List<BaseUser> result) {
		super.onPostExecute(result);
		btnDelete.setEnabled(true);
		try {
		     dialog.dismiss();
		} catch (Exception e) {}

		if (ListUtil.isEmpty(result)) {
			return;
		}

		UserGroupDao ugDao = new UserGroupDao(context);
		for (BaseUser user : result) {
			UserGroup ug = new UserGroup();
			ug.setUserId(user.getId());
			ug.setGroupId(group.getGroupId());
			ug.setServiceProviderNo(account.getServiceProviderNo());
			ug.setState(UserGroup.STATE_ADDED);
			ugDao.delete(ug);
		}

		for (BaseUser user : result) {
			adapter.remove(user);
		}

		String msg = context.getString(
			R.string.msg_group_member_delete_success, result.size());
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}

	private OnCancelListener onCancelListener = new OnCancelListener() {
		public void onCancel(DialogInterface dialog) {
			GroupMemberDeleteTask.this.cancel(true);
		}
	};

}
