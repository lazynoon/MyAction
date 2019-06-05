package net_io.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class StatNIO {
	/** 启动时间 **/
	public final static Date startTime = new Date();
	/** 核心类事件统计 **/
	public final static BossClass bossClass = new BossClass();
	/** StreamSocket类事件统计 **/
	public final static StreamSocketStat streamStat = new StreamSocketStat();
	/** PacketSocket类事件统计 **/
	public final static PacketSocketStat packetStat = new PacketSocketStat();
	
	/** 核心类统计参数订单 **/
	public static class BossClass {
		private ArrayList<String> fields = new ArrayList<String>();
		private HashMap<String, String> chineseNames = new HashMap<String, String>();
		private BossClass() {
			init();
		}
		/** 创建异步Socket对象 **/
		protected AtomicLong create_aync_socket = new AtomicLong(0);
		/** 侦听端口调用开始 **/
		protected AtomicLong bind_invoke_start = new AtomicLong(0);
		/** 侦听端口调用结束 **/
		protected AtomicLong bind_invoke_end = new AtomicLong(0);
		/** 取消端口侦听调用 **/
		protected AtomicLong unbind_invoke_start = new AtomicLong(0);
		/** 取消未侦听的端口 **/
		protected AtomicLong unbind_not_exist = new AtomicLong(0);
		/** 取消端口侦听结束 **/
		protected AtomicLong unbind_invoke_end = new AtomicLong(0);
		/** 连接调用开始 **/
		protected AtomicLong connect_invoke_start = new AtomicLong(0);
		/** 连接调用结束 **/
		protected AtomicLong connect_invoke_end = new AtomicLong(0);
		/** 在BOSS线程发起连接开始 **/
		protected AtomicLong do_connect_start = new AtomicLong(0);
		/** 在BOSS线程发起连接结束 **/
		protected AtomicLong do_connect_end = new AtomicLong(0);
		/** 在BOSS线程连接立即完成 **/
		protected AtomicLong do_connect_immediate = new AtomicLong(0);
		/** 在BOSS线程连接立即完成 **/
		protected AtomicLong do_connect_finish = new AtomicLong(0);
		/** 关闭SocketChannel开始 **/
		protected AtomicLong close_channel_start = new AtomicLong(0);
		/** 正常关闭Socket **/
		protected AtomicLong close_socket_pass = new AtomicLong(0);
		/** 关闭Socket出现异常 **/
		protected AtomicLong close_socket_error = new AtomicLong(0);
		/** 取消NIO事件 **/
		protected AtomicLong close_selection_cancel = new AtomicLong(0);
		/** 关闭时NIO事件不存在 **/
		protected AtomicLong close_selection_null = new AtomicLong(0);
		/** 关闭NetChannel **/
		protected AtomicLong close_net_channel = new AtomicLong(0);
		/** NetChannel进入CLOSE_WAIT状态 **/
		protected AtomicLong close_goto_wait = new AtomicLong(0);
		/** onClose回调错误 **/
		protected AtomicLong close_callback_error = new AtomicLong(0);
		/** 两次调用连接关闭 **/
		protected AtomicLong close_twice_invoke = new AtomicLong(0);
		/** 关闭SocketChannel开始 **/
		protected AtomicLong close_channel_end = new AtomicLong(0);
		/** 创建Boss线程 **/
		protected AtomicLong boss_thread_create = new AtomicLong(0);
		/** Boss线程运行一次 **/
		protected AtomicLong boss_run_once = new AtomicLong(0);
		/** 在Boss线程结束前关闭的SocketChannel **/
		protected AtomicLong boss_selection_close = new AtomicLong(0);
		/** 注册接受连接事件 **/
		protected AtomicLong event_register_accept = new AtomicLong(0);
		/** 注册连接连接事件 **/
		protected AtomicLong event_register_connect = new AtomicLong(0);
		/** Boss线程连接操作失败 **/
		protected AtomicLong event_doconnect_error = new AtomicLong(0);
		/** Boss为找到连接回调 **/
		protected AtomicLong event_no_onconnect = new AtomicLong(0);
		/** 事件注册失败 **/
		protected AtomicLong event_register_error = new AtomicLong(0);
		/** 收到NIO事件 **/
		protected AtomicLong event_active_selection = new AtomicLong(0);
		/** 错误的NIO事件选择器 **/
		protected AtomicLong event_invalid_selection = new AtomicLong(0);
		/** 服务端收到连接请求 **/
		protected AtomicLong event_acceptable_selection = new AtomicLong(0);
		/** 客户端收到连接回应 **/
		protected AtomicLong event_connectable_selection = new AtomicLong(0);
		/** 可发送数据的事件 **/
		protected AtomicLong event_writable_selection = new AtomicLong(0);
		/** 可接收数据的事件 **/
		protected AtomicLong event_readable_selection = new AtomicLong(0);
		/** 收到NIO事件，但找不到对应的NetChannel **/
		protected AtomicLong event_unregister_channel = new AtomicLong(0);
		/** 服务端不接受连接 **/
		protected AtomicLong run_not_accept = new AtomicLong(0);
		/** 连接第2段“建立中” **/
		protected AtomicLong run_connection_pedding = new AtomicLong(0);
		/** 完成连接 **/
		protected AtomicLong run_finish_connect = new AtomicLong(0);
		/** 发送的缓存包 **/
		protected AtomicLong run_write_buff = new AtomicLong(0);
		/** 发送的字节数 **/
		protected AtomicLong run_write_size = new AtomicLong(0);
		/** 发送0字节 **/
		protected AtomicLong run_write_zero = new AtomicLong(0);
		/** 未发送全部数据 **/
		protected AtomicLong run_has_remaining = new AtomicLong(0);
		/** 移除发送事件 **/
		protected AtomicLong run_remove_wriable = new AtomicLong(0);
		/** 接收数据时，Socket已关闭 **/
		protected AtomicLong run_readsocket_closed = new AtomicLong(0);
		/** Boss主线程中的IOException **/
		protected AtomicLong run_io_exception = new AtomicLong(0);
		/** Boss主线程中的其它Exception **/
		protected AtomicLong run_other_exception = new AtomicLong(0);
	
		private void init() {
			addName("create_aync_socket", "创建异步Socket对象");
			addName("bind_invoke_start", "侦听端口调用开始");
			addName("bind_invoke_end", "侦听端口调用结束");
			addName("unbind_invoke_start", "取消端口侦听调用");
			addName("unbind_not_exist", "取消未侦听的端口");
			addName("unbind_invoke_end", "取消端口侦听结束");
			addName("connect_invoke_start", "连接调用开始");
			addName("connect_invoke_end", "连接调用结束");
			addName("do_connect_start", "在BOSS线程发起连接开始");
			addName("do_connect_end", "在BOSS线程发起连接结束");
			addName("do_connect_immediate", "在BOSS线程连接立即完成");
			addName("do_connect_finish", "在BOSS线程连接立即完成");
			addName("close_channel_start", "关闭SocketChannel开始");
			addName("close_socket_pass", "正常关闭Socket");
			addName("close_socket_error", "关闭Socket出现异常");
			addName("close_selection_cancel", "取消NIO事件");
			addName("close_selection_null", "关闭时NIO事件不存在");
			addName("close_net_channel", "关闭NetChannel");
			addName("close_goto_wait", "NetChannel进入CLOSE_WAIT状态");
			addName("close_callback_error", "onClose回调错误");
			addName("close_twice_invoke", "两次调用连接关闭");
			addName("close_channel_end", "关闭SocketChannel开始");
			addName("boss_thread_create", "创建Boss线程");
			addName("boss_run_once", "Boss线程运行一次");
			addName("boss_selection_close", "在Boss线程结束前关闭的SocketChannel");
			addName("event_register_accept", "注册接受连接事件");
			addName("event_register_connect", "注册连接连接事件");
			addName("event_doconnect_error", "Boss线程连接操作失败");
			addName("event_no_onconnect", "Boss为找到连接回调");
			addName("event_register_error", "事件注册失败");
			addName("event_active_selection", "收到NIO事件");
			addName("event_invalid_selection", "错误的NIO事件选择器");
			addName("event_acceptable_selection", "服务端收到连接请求");
			addName("event_connectable_selection", "客户端收到连接回应");
			addName("event_writable_selection", "可发送数据的事件");
			addName("event_readable_selection", "可接收数据的事件");
			addName("event_unregister_channel", "收到NIO事件，但找不到对应的NetChannel");
			addName("run_not_accept", "服务端不接受连接");
			addName("run_connection_pedding", "连接第2段“建立中”");
			addName("run_finish_connect", "完成连接");
			addName("run_write_buff", "发送的缓存包");
			addName("run_write_size", "发送的字节数");
			addName("run_write_zero", "发送0字节");
			addName("run_has_remaining", "未发送全部数据");
			addName("run_remove_wriable", "移除发送事件");
			addName("run_readsocket_closed", "接收数据时，Socket已关闭");
			addName("run_io_exception", "Boss主线程中的IOException");
			addName("run_other_exception", "Boss主线程中的其它Exception");
		}
		
		private void addName(String code, String name) {
			fields.add(code);
			chineseNames.put(code, name);
		}
		
		/**
		 * 获取所有字段列表
		 * @return 
		 */
		public String[] getFields() {
			return fields.toArray(new String[0]);
		}
		
		/**
		 * 获取字段的中文名称
		 * @param field
		 * @return
		 */
		public String getChineseName(String field) {
			return chineseNames.get(field);
		}
	
		/**
		 * 获取统计数据
		 * @return Map
		 */
		public Map<String, Long> getStat() {
			Map<String, Long> map = new HashMap<String, Long>();
			
			map.put("create_aync_socket", create_aync_socket.get());
			map.put("bind_invoke_start", bind_invoke_start.get());
			map.put("bind_invoke_end", bind_invoke_end.get());
			map.put("unbind_invoke_start", unbind_invoke_start.get());
			map.put("unbind_not_exist", unbind_not_exist.get());
			map.put("unbind_invoke_end", unbind_invoke_end.get());
			map.put("connect_invoke_start", connect_invoke_start.get());
			map.put("connect_invoke_end", connect_invoke_end.get());
			map.put("do_connect_start", do_connect_start.get());
			map.put("do_connect_end", do_connect_end.get());
			map.put("do_connect_immediate", do_connect_immediate.get());
			map.put("do_connect_finish", do_connect_finish.get());
			map.put("close_channel_start", close_channel_start.get());
			map.put("close_socket_pass", close_socket_pass.get());
			map.put("close_socket_error", close_socket_error.get());
			map.put("close_selection_cancel", close_selection_cancel.get());
			map.put("close_selection_null", close_selection_null.get());
			map.put("close_net_channel", close_net_channel.get());
			map.put("close_goto_wait", close_goto_wait.get());
			map.put("close_callback_error", close_callback_error.get());
			map.put("close_twice_invoke", close_twice_invoke.get());
			map.put("close_channel_end", close_channel_end.get());
			map.put("boss_thread_create", boss_thread_create.get());
			map.put("boss_run_once", boss_run_once.get());
			map.put("boss_selection_close", boss_selection_close.get());
			map.put("event_register_accept", event_register_accept.get());
			map.put("event_register_connect", event_register_connect.get());
			map.put("event_doconnect_error", event_doconnect_error.get());
			map.put("event_no_onconnect", event_no_onconnect.get());
			map.put("event_register_error", event_register_error.get());
			map.put("event_active_selection", event_active_selection.get());
			map.put("event_invalid_selection", event_invalid_selection.get());
			map.put("event_acceptable_selection", event_acceptable_selection.get());
			map.put("event_connectable_selection", event_connectable_selection.get());
			map.put("event_writable_selection", event_writable_selection.get());
			map.put("event_readable_selection", event_readable_selection.get());
			map.put("event_unregister_channel", event_unregister_channel.get());
			map.put("run_not_accept", run_not_accept.get());
			map.put("run_connection_pedding", run_connection_pedding.get());
			map.put("run_finish_connect", run_finish_connect.get());
			map.put("run_write_buff", run_write_buff.get());
			map.put("run_write_size", run_write_size.get());
			map.put("run_write_zero", run_write_zero.get());
			map.put("run_has_remaining", run_has_remaining.get());
			map.put("run_remove_wriable", run_remove_wriable.get());
			map.put("run_readsocket_closed", run_readsocket_closed.get());
			map.put("run_io_exception", run_io_exception.get());
			map.put("run_other_exception", run_other_exception.get());
			
			//运行时间（单位ms）
			map.put("offset_time", new Date().getTime() - startTime.getTime());
			return map;
		}
	}

	/** StreamSocket统计 **/
	public static class StreamSocketStat {
		private ArrayList<String> fields = new ArrayList<String>();
		private HashMap<String, String> chineseNames = new HashMap<String, String>();
		private StreamSocketStat() {
			init();
		}
		/** 创建StreamSocket对象 **/
		protected AtomicLong create_stream_socket = new AtomicLong(0);
		/** 底层回调连接成功 **/
		protected AtomicLong call_on_connect = new AtomicLong(0);
		/** 底层回调接收数据 **/
		protected AtomicLong call_on_receive = new AtomicLong(0);
		/** 底层回调连接关闭 **/
		protected AtomicLong call_on_close = new AtomicLong(0);
		/** 检查是否接受连接 **/
		protected AtomicLong default_on_accept = new AtomicLong(0);
		/** 默认处理连接成功 **/
		protected AtomicLong default_on_connect = new AtomicLong(0);
		/** 默认处理接收数据件 **/
		protected AtomicLong default_on_receive = new AtomicLong(0);
		/** 默认处理连接关闭 **/
		protected AtomicLong default_on_close = new AtomicLong(0);
		/** 发送数据包调用 **/
		protected AtomicLong send_invoke = new AtomicLong(0);
		/** 发送的字节数 **/
		protected AtomicLong send_size = new AtomicLong(0);
		/** 接收的字节数 **/
		protected AtomicLong receive_size = new AtomicLong(0);
	
		private void init() {
			addName("create_stream_socket", "创建StreamSocket对象");
			addName("call_on_connect", "底层回调连接成功");
			addName("call_on_receive", "底层回调接收数据");
			addName("call_on_close", "底层回调连接关闭");
			addName("default_on_accept", "检查是否接受连接");
			addName("default_on_connect", "默认处理连接成功");
			addName("default_on_receive", "默认处理接收数据");
			addName("default_on_close", "默认处理连接关闭");
			addName("send_invoke", "发送数据包调用");
			addName("send_size", "发送的字节数");
			addName("receive_size", "接收的字节数");
		}
		
		private void addName(String code, String name) {
			fields.add(code);
			chineseNames.put(code, name);
		}
		
		/**
		 * 获取所有字段列表
		 * @return 
		 */
		public String[] getFields() {
			return fields.toArray(new String[0]);
		}
		
		/**
		 * 获取字段的中文名称
		 * @param field
		 * @return
		 */
		public String getChineseName(String field) {
			return chineseNames.get(field);
		}
	
		/**
		 * 获取统计数据
		 * @return Map
		 */
		public Map<String, Long> getStat() {
			Map<String, Long> map = new HashMap<String, Long>();
			
			map.put("create_stream_socket", create_stream_socket.get());
			map.put("call_on_connect", call_on_connect.get());
			map.put("call_on_receive", call_on_receive.get());
			map.put("call_on_close", call_on_close.get());
			map.put("default_on_accept", default_on_accept.get());
			map.put("default_on_connect", default_on_connect.get());
			map.put("default_on_receive", default_on_receive.get());
			map.put("default_on_close", default_on_close.get());
			map.put("send_invoke", send_invoke.get());
			map.put("send_size", send_size.get());
			map.put("receive_size", receive_size.get());

			//运行时间（单位ms）
			map.put("offset_time", new Date().getTime() - startTime.getTime());
			return map;
		}
	}

	/** PacketSocket统计 **/
	public static class PacketSocketStat {
		private ArrayList<String> fields = new ArrayList<String>();
		private HashMap<String, String> chineseNames = new HashMap<String, String>();
		private PacketSocketStat() {
			init();
		}
		/** 创建PacketSocketStat对象 **/
		protected AtomicLong create_packet_socket = new AtomicLong(0);
		/** 底层回调连接成功 **/
		protected AtomicLong call_on_connect = new AtomicLong(0);
		/** 底层回调接收数据件 **/
		protected AtomicLong call_on_receive = new AtomicLong(0);
		/** 底层回调连接关闭 **/
		protected AtomicLong call_on_close = new AtomicLong(0);
		/** 检查是否接受连接 **/
		protected AtomicLong default_on_accept = new AtomicLong(0);
		/** 默认处理连接成功 **/
		protected AtomicLong default_on_connect = new AtomicLong(0);
		/** 默认处理接收数据件 **/
		protected AtomicLong default_on_receive = new AtomicLong(0);
		/** 默认处理连接关闭 **/
		protected AtomicLong default_on_close = new AtomicLong(0);
		/** 发送数据包调用 **/
		protected AtomicLong send_invoke = new AtomicLong(0);
		/** 发送的字节数 **/
		protected AtomicLong send_size = new AtomicLong(0);
		/** 接收的字节数 **/
		protected AtomicLong receive_size = new AtomicLong(0);
		/** 调用底层read次数 **/
		protected AtomicLong read_times = new AtomicLong(0);
		/** 读取到连接关闭事件 **/
		protected AtomicLong read_close = new AtomicLong(0);
		/** 读取到0字节 **/
		protected AtomicLong read_zero = new AtomicLong(0);
		/** 读取到空包 **/
		protected AtomicLong msg_empty_package = new AtomicLong(0);
		/** 创建BaseMsg调用 **/
		protected AtomicLong msg_create_invoke = new AtomicLong(0);
		/** 消息包未读满 **/
		protected AtomicLong msg_wait_read = new AtomicLong(0);
		/** 未注册的消息 **/
		protected AtomicLong msg_undefined = new AtomicLong(0);
		/** 消息处理 **/
		protected AtomicLong msg_process = new AtomicLong(0);
		/** 错误的数据包 **/
		protected AtomicLong error_format_packet = new AtomicLong(0);
	
		private void init() {
			addName("create_packet_socket", "创建PacketSocket对象");
			addName("call_on_connect", "底层回调连接成功");
			addName("call_on_receive", "底层回调接收数据");
			addName("call_on_close", "底层回调连接关闭");
			addName("default_on_accept", "检查是否接受连接");
			addName("default_on_connect", "默认处理连接成功");
			addName("default_on_receive", "默认处理接收数据");
			addName("default_on_close", "默认处理连接关闭");
			addName("send_invoke", "发送数据包调用");
			addName("send_size", "发送的字节数");
			addName("receive_size", "接收的字节数");
			addName("read_times", "调用底层read次数 ");
			addName("read_close", "读取到连接关闭事件");
			addName("read_zero", "读取到0字节");
			addName("msg_empty_package", "读取到空包");
			addName("msg_create_invoke", "创建BaseMsg调用");
			addName("msg_wait_read", "消息包未读满");
			addName("msg_undefined", "未注册的消息");
			addName("msg_process", "消息处理");
			addName("error_format_packet", "错误的数据包");
		}
		
		private void addName(String code, String name) {
			fields.add(code);
			chineseNames.put(code, name);
		}
		
		/**
		 * 获取所有字段列表
		 * @return 
		 */
		public String[] getFields() {
			return fields.toArray(new String[0]);
		}
		
		/**
		 * 获取字段的中文名称
		 * @param field
		 * @return
		 */
		public String getChineseName(String field) {
			return chineseNames.get(field);
		}
	
		/**
		 * 获取统计数据
		 * @return Map
		 */
		public Map<String, Long> getStat() {
			Map<String, Long> map = new HashMap<String, Long>();
			
			map.put("create_packet_socket", create_packet_socket.get());
			map.put("call_on_connect", call_on_connect.get());
			map.put("call_on_receive", call_on_receive.get());
			map.put("call_on_close", call_on_close.get());
			map.put("default_on_accept", default_on_accept.get());
			map.put("default_on_connect", default_on_connect.get());
			map.put("default_on_receive", default_on_receive.get());
			map.put("default_on_close", default_on_close.get());
			map.put("send_invoke", send_invoke.get());
			map.put("send_size", send_size.get());
			map.put("receive_size", receive_size.get());
			map.put("read_times", read_times.get());
			map.put("read_close", read_close.get());
			map.put("read_zero", read_zero.get());
			map.put("msg_empty_package", msg_empty_package.get());
			map.put("msg_create_invoke", msg_create_invoke.get());
			map.put("msg_wait_read", msg_wait_read.get());
			map.put("msg_undefined", msg_undefined.get());
			map.put("msg_process", msg_process.get());
			map.put("error_format_packet", error_format_packet.get());

			//运行时间（单位ms）
			map.put("offset_time", new Date().getTime() - startTime.getTime());
			return map;
		}
	}
}
