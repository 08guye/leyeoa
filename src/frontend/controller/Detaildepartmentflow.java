package frontend.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import rbac.dao.D_Role_Hierarchy;
import rbac.javabean.RbacAccount;
import rbac.javabean.RbacRole;
import frontend.dao.D_FinishedFlow;
import frontend.javabean.Workflow;

/**
 * Servlet implementation class Detaildepartmentflow
 */
public class Detaildepartmentflow extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		HashMap<Integer, RbacAccount> rbac = (HashMap<Integer, RbacAccount>) getServletContext()
				.getAttribute("rbac");
		HashMap<Integer, RbacRole> roles = (HashMap<Integer, RbacRole>) getServletContext()
				.getAttribute("roles");

		String flowid = request.getParameter("flowid");
		int accountid = (Integer) request.getSession().getAttribute("id");

		boolean approval = false;
		int approvalRoleId = 0;

		// 可审批用户都可查询本部已审批单据
		// 如果我有Approval角色

		if (rbac.get(accountid).getRole().contains("Approval")) {
			approval = true;
		} else {
			Set<Integer> roleskey = roles.keySet();
			for (Integer key : roleskey) {
				if (roles.get(key).getName().equals("Approval")) {
					approvalRoleId = key;
					break;
				}
			}
			if (approvalRoleId != 0) {
				ArrayList<Integer> basicRole = D_Role_Hierarchy
						.doSelectAdvanced(approvalRoleId);
				for (int id : basicRole) {
					if (roles.get(id).getUser().containsKey(accountid)) {
						approval = true;
						break;
					}
				}
			}
		}

		if (approval == false) {
			if (rbac.get(accountid).getRole().contains("DepartmentForm")) {
				approval = true;
			}
		}

		if (approval == true) {

			int default_roleid = rbac.get(accountid).getDefault_roleid();

			Workflow workflow = null;

			if (flowid != null) {
				workflow = D_FinishedFlow.doSelectDepartmentDetail(
						default_roleid, Integer.valueOf(flowid));
			}

			LinkedHashMap<String, String> finishInfo = null;
			if (workflow != null) {
				if (workflow.getAccountflow() != null) {
					String accountFlow[] = workflow.getAccountflow().split(",");
					finishInfo = new LinkedHashMap<String, String>();

					for (String account : accountFlow) {
						String defaultRole = roles.get(
								rbac.get(Integer.valueOf(account))
										.getDefault_roleid()).getAlias();
						String accountName = rbac.get(Integer.valueOf(account))
								.getFullname();
						finishInfo
								.put(account, defaultRole + "-" + accountName);
					}
				}
			} else {
				response.sendRedirect("rdepartmentflow");
				return;
			}

			String accountDefaultRole = roles.get(
					rbac.get(Integer.valueOf(workflow.getAccount_id()))
							.getDefault_roleid()).getAlias();
			String accountName = rbac.get(
					Integer.valueOf(workflow.getAccount_id())).getFullname();

			String accountInfo = accountDefaultRole + "-" + accountName;

			request.setAttribute("accountInfo", accountInfo);
			request.setAttribute("finishInfo", finishInfo);
			request.setAttribute("content", workflow.getContent());
			request.setAttribute("status", workflow.getStatus());

			String url = "/WEB-INF/frontend/detailfinishedflow.jsp";
			RequestDispatcher dispatcher = getServletContext()
					.getRequestDispatcher(url);
			dispatcher.forward(request, response);
		} else {
			response.sendRedirect("rdepartmentflow");
		}
	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		this.doGet(request, response);
	}

}
