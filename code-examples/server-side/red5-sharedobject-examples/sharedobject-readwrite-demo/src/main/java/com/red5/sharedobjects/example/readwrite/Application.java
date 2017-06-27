package com.red5.sharedobjects.example.readwrite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IAttributeStore;
import org.red5.server.api.IConnection;
import org.red5.server.api.IServer;
import org.red5.server.api.listeners.IConnectionListener;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectBase;
import org.red5.server.api.so.ISharedObjectListener;
import org.red5.server.scope.WebScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Application extends MultiThreadedApplicationAdapter {

	
	private static Logger log = LoggerFactory.getLogger(Application.class);

	private ISharedObject gameRoom = null;
	
	private IScope appScope;
	
	
	@Override
	public boolean appStart(IScope app) {
		log.info("Application started : {}", app);
		try 
		{
			this.appScope = app;
			
			/* Add connection listener */
			
			WebScope scope = (WebScope) app;
			IServer server = scope.getServer();
			server.addListener(connectionListener);
			
			
			/* Get shared object instance */
			gameRoom = getGameRoomSharedObject(app, "gameroom", false);
			
			
			/** Lock and unlock are needed only if you do not want any other thread to operate 
			 * on the shared object while you are operating on it. Unlock the shared object after you are done. */
			gameRoom.lock();
			gameRoom.acquire();
			gameRoom.addSharedObjectListener(roomListener);
			gameRoom.unlock();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		return super.appStart(app);
	}
	
	
	
	
	/**
	 * Listen for successful connection to the application
	 */
	private IConnectionListener connectionListener = new IConnectionListener(){

		@Override
		public void notifyConnected(IConnection conn) {
			
			/* Not our scope then NVM */
			if(conn.getScope() != appScope){
				return;
			}
			
			String username = conn.getStringAttribute("username") + "-" + conn.getSessionId();
			
			if(username != null){
			
				List<String> users = (List<String>) gameRoom.getListAttribute("users");
				if(users == null) {
					users = new ArrayList<String>();
				}
				
				users.add(username);
				gameRoom.setAttribute("users", users);
				gameRoom.setDirty(true);
			}
		}

		
		
		@Override
		public void notifyDisconnected(IConnection conn) {
			
			/* Not our scope then NVM */
			if(conn.getScope() != appScope){
				return;
			}
			
			String username = conn.getStringAttribute("username") + "-" + conn.getSessionId();
			
			if(username != null){
			
				List<String> users = (List<String>) gameRoom.getListAttribute("users");
				if(users == null) {
					users = new ArrayList<String>();
				}
				
				users.remove(username);
				gameRoom.setAttribute("users", users);
				gameRoom.setDirty(true);
			}
		}
		
		
	};

	
	
	
	
	/**
	 * 
	 * @param scope
	 * @param name
	 * @param persistent
	 * @return
	 * @throws Exception
	 */
	private ISharedObject getGameRoomSharedObject(IScope scope, String name, boolean persistent) throws Exception{
	
		ISharedObject so = getSharedObject(scope, name, persistent);
		
		if(so == null){
			createSharedObject(scope, name, persistent);
			
			/* Check again */
			so = getSharedObject(scope, name, persistent);
			if(so == null) throw new Exception("Shared object was not created");
		}
		
		return so;
	}

	
	
	
	
	/**
	 * Destroys the shared object created by the 'initSharedObject' call
	 * 
	 * @param scope
	 * @param name
	 * @param persistent
	 */
	private void destroySharedObject(){
	
		if(gameRoom != null)
		{
			gameRoom.removeSharedObjectListener(roomListener);
			gameRoom.release();
		}
		
		gameRoom = null;
	}

	
	
	
	
	/**
	 * Shared Object listener object for monitoring shared object events
	 */
	private ISharedObjectListener roomListener  = new ISharedObjectListener(){

		/*
		 * (non-Javadoc)
		 * @see org.red5.server.api.so.ISharedObjectListener#onSharedObjectConnect(org.red5.server.api.so.ISharedObjectBase)
		 */
		@Override
		public void onSharedObjectConnect(ISharedObjectBase so) {
			log.info("Client connected to the shared object");			
		}

		
		/*
		 * (non-Javadoc)
		 * @see org.red5.server.api.so.ISharedObjectListener#onSharedObjectDisconnect(org.red5.server.api.so.ISharedObjectBase)
		 */
		@Override
		public void onSharedObjectDisconnect(ISharedObjectBase so) {
			log.info("Client disconnected from the shared object");
		}

		
		/*
		 * (non-Javadoc)
		 * @see org.red5.server.api.so.ISharedObjectListener#onSharedObjectUpdate(org.red5.server.api.so.ISharedObjectBase, java.lang.String, java.lang.Object)
		 */
		@Override
		public void onSharedObjectUpdate(ISharedObjectBase so, String key, Object value) {
			log.info("Shared object property {} is updated", key);
		}

		
		
		/*
		 * (non-Javadoc)
		 * @see org.red5.server.api.so.ISharedObjectListener#onSharedObjectUpdate(org.red5.server.api.so.ISharedObjectBase, org.red5.server.api.IAttributeStore)
		 */
		@Override
		public void onSharedObjectUpdate(ISharedObjectBase so, IAttributeStore values) {
			log.info("Shared object attribute store is updated");
		}

		
		
		/*
		 * (non-Javadoc)
		 * @see org.red5.server.api.so.ISharedObjectListener#onSharedObjectUpdate(org.red5.server.api.so.ISharedObjectBase, java.util.Map)
		 */
		@Override
		public void onSharedObjectUpdate(ISharedObjectBase so, Map<String, Object> map) {
			log.info("Shared object multiple properties are updated");
			for (Map.Entry<String, Object> entry : map.entrySet())
			{
			    log.info(entry.getKey() + "/" + entry.getValue());
			}
		}

		
		
		
		/*
		 * 		(non-Javadoc)
		 * @see org.red5.server.api.so.ISharedObjectListener#onSharedObjectDelete(org.red5.server.api.so.ISharedObjectBase, java.lang.String)
		 */
		@Override
		public void onSharedObjectDelete(ISharedObjectBase so, String key) {
			log.info("Property {} deleted from shared object", key);
		}
		
		
		
		/*
		 * (non-Javadoc)
		 * @see org.red5.server.api.so.ISharedObjectListener#onSharedObjectClear(org.red5.server.api.so.ISharedObjectBase)
		 */
		@Override
		public void onSharedObjectClear(ISharedObjectBase so) {
			log.info("Shared object cleared");
		}

		
		
		/*
		 * (non-Javadoc)
		 * @see org.red5.server.api.so.ISharedObjectListener#onSharedObjectSend(org.red5.server.api.so.ISharedObjectBase, java.lang.String, java.util.List)
		 */
		@Override
		public void onSharedObjectSend(ISharedObjectBase so, String method, List<?> params) {
			log.info("Shared Object send called for method {}", method);
			
		}
		
	};

	
	
	
	
	@Override
	public boolean appConnect(IConnection conn, Object[] params) {
		log.info("Client connect : {}",  conn);
		return super.appConnect(conn, params);
	}







	@Override
	public void appDisconnect(IConnection conn) {
		log.info("Client disconnect : {}",  conn);
		super.appDisconnect(conn);
	}







	@Override
	public void appStop(IScope arg0) {
		log.info("Application stopped : {}", arg0);
		destroySharedObject();
		super.appStop(arg0);
	}
}
