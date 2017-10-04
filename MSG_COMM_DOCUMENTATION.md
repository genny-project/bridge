####  This is the documentation file for the message communication between Front End (Alyson) and BackEnd(Bridge)  ####
## Project name: Genny-Project ##

#### Basic Message Communication rules  ####
The Front-End will always send event message (EVT_MSG) for any action to the Backend and backend will in-return send the command message (CMD_MSG) or data message (DATA_MSG) or sometimes both to the front end.


### Component: User Login ###
While user logs in, the front-end sends event message (EVT_MSG) with the code "AUTH_INIT".
 
 **Event Messsage for User login** 
  ```json
     {
        msg_type: “EVT_MSG”,
        evt_type: “AUTH_INIT”,
        data: 
              { 
    		  code: “AUTH_INIT”
              }    
     }
   ```
 
     
 The Backend in return sends CMD_MSG with the properties of the layout to be displayed and the DATA_MSG with the base entities to be displayed in the TreeView Component/Layout.
  **Command Message from BackEnd**  
   ```json
      {
         msg_type: “CMD_MSG”,
         cmd_type: “CMD_LAYOUT”,
         code: “layout1”,
         data:{
                   layout: [{
		            “Grid”:{
                           [ Layouts_Properties ]
                                      }
                                 }
                              ]
               }
       }
    ```
    AND
    **DATA Message with base entities to be displayed in the TreeView**
    ```json
       {
           msg_type: “DATA_MSG”,
           data_type: “BaseEntity”,
           delete: “false”,
           items: [
	       {
	          {
		     created: "2017-10-04T02:43:56",
		     updated: "2017-10-04T02:43:56",
		     id: 2,
		     name: "Live-View",    --> This is the string/value to be displayed in the Tree View
		     code: "GRP_LIVE_VIEW" --> This is the code of this particular treeview item to be send with event to BE
		   },
		   {
		      created: "2017-10-04T02:43:56",
		      updated: "2017-10-04T02:43:56",
                      id: 3,
                      name: "Loads",
                      code: "GRP_LOADS"
                   },
                   {
                      created: "2017-10-04T02:43:56",
                      updated: "2017-10-04T02:43:56",
                      id: 4,
                      name: "Contacts",
                      code: "GRP_CONTACTS"
                    },
                    {
                      created: "2017-10-04T02:43:56",
                      updated: "2017-10-04T02:43:56",
                      id: 5,
                      name: "Companies",
                      code: "GRP_COMPANYS"
                    },
                    {
			created: "2017-10-04T02:43:56",
			updated: "2017-10-04T02:43:56",
			id: 6,
			name: "Users",
			code: "GRP_USERS"
		     },
		     {
			created: "2017-10-04T02:43:56",
			updated: "2017-10-04T02:43:56",
			id: 7,
			name: "Settings",
			code: "GRP_SETTINGS"
		      }
		   ],
	    parentCode: "GRP_ROOT",    --> This part identifies the parent item
	   linkCode: "LNK_CORE" .     --> Provides relationship between parent and child items
         }
      ```
	            

### Component: TreeView ###
TreeView component will send following events for each different actions on its item

  | Actions  		            | EVENTS to BackEnd  (event_type) |
  | --------------------------- | ------------------- |       
  | Select/Click on treeview item    |  TV_SELECT    |
  | Click on Expand icon             |  TV_EXPAND    |
  | Click on Contract icon           |  TV_CONTRACT  |
  | Drag and Drop of the treeview item | TV_DRAG_DROP |
  | Multi selection of the treeview item  | TV_MULTI_SELECTION |
  | Right click on the treeview item (Only available for Admin user  |  TV_EDIT |
   
   ## Example of the EVENT Message to be sent from Front-End in JSON Format: ##
 
 **Event Message for click on Expand icon in TreeView**   
  ```json
    {
      msg_type : "EVT_MSG",
      event_type : "TV_EXPAND",
      data:
          {
             code: "TV1"    -----> Here, TV1 stands for TreeView1, considering there can be multiple TreeView components
             value: "GRP_LIVE_VIEW"   -----> This is the actual code of the TreeView item (Treeview ParentNode)
           }
     }
   ```
   
 On Return the back-end will send the following DATA_MSG with all the child Base Entity in JSON Format:
  ```json
     {
        msg_type : "DATA_MSG",
        data_type : "BaseEntity",
        delete    : false,
        items  : [
                {
                    created: "2017-10-03T22:59:21",
                    updated: "2017-10-03T22:59:21",
                    id: 8,
                    name: "Pending",   ---> This is the name to be displayed in the TreeView
		    code: "GRP_PENDING"  --->  This is the code of this item, which is needed to be send to the Backend as a value in the EVT_MSG
                 },
                 {
		      created: "2017-10-03T22:59:22",
         	      updated: "2017-10-03T22:59:22",
			id: 9,
			name: "Accepted",
			 code: "GRP_ACCEPTED"
		},
		{
		        created: "2017-10-03T22:59:22",
			updated: "2017-10-03T22:59:22",
			id: 10,
			name: "Dispatched",
			code: "GRP_DISPATCHED"
				},
				{
					created: "2017-10-03T22:59:22",
					updated: "2017-10-03T22:59:22",
					id: 11,
					name: "In-Transit",
					code: "GRP_IN-TRANSIT"
			    },
			    {
					created: "2017-10-03T22:59:22",
					updated: "2017-10-03T22:59:22",
					id: 12,
					name: "At-Destination",
					code: "GRP_AT-DESTINATION"
				},
				{
					created: "2017-10-03T22:59:22",
				    updated: "2017-10-03T22:59:22",
					id: 13,
					name: "Delivered",
					code: "GRP_DELIVERED"
				}
		     ],
		     parentCode: "GERP_LIVE_VIEW", ---> This is the code of the Parent item to which these child entity belongs to
		     linkCode: "LNK_CORE"  ---> Here, it is the relationship code between the parent and child
        }
    ```


**Event Message for click/select on TreeView Item**
```json
    {
      msg_type : "EVT_MSG",
      evt_type : "TV_SELECT",   -----> Code for the treeview item click
      data:
          {
             code: "TV1"    -----> Here, TV1 stands for TreeView1, considering there can be multiple TreeView components
             value: "GRP_LIVE_VIEW"   -----> This is the actual code of the TreeView item (Treeview ParentNode)
           }
     }
 ```
    On Return the back-end will send the following CMD_MSG with code in JSON Format:
 ```json
      {
          msg_type : "CMD_MSG",
          cmd_type : "TV_SELECT",
          code    : "GRP_LIVE_VIEW"
       }
  ```
      
 **Event Message for click on Contract icon in TreeView**
 ```json
    {
      msg_type : "EVT_MSG",
      event_type : "TV_CONTRACT",
      data:
          {
             code: "TV1"    -----> Here, TV1 stands for TreeView1, considering there can be multiple TreeView components
             value: "GRP_LIVE_VIEW"   -----> This is the actual code of the TreeView item (Treeview ParentNode)
           }
     }
 ```  
On Return the back-end will send the following DATA_MSG with all the child Base Entity in JSON Format:
```json
     {
        msg_type : "CMD_MSG",
	cmd_type : "TV_CONTRACT",
	code : "GRP_LIVE_VIEW"
      }
   ```
