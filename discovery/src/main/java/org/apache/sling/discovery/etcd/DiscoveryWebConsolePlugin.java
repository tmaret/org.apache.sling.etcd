/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.discovery.etcd;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.discovery.etcd.fsm.Context;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.etcd.client.FollowerStats;
import org.apache.sling.etcd.client.LeaderStatsResponse;
import org.apache.sling.etcd.client.Member;
import org.apache.sling.etcd.client.MemberStatsResponse;
import org.osgi.framework.Constants;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;

@Component
@Service
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION,
                value = "CoreOS etcd based Discovery Service Web Console"),
        @Property(name = WebConsoleConstants.PLUGIN_LABEL, value = DiscoveryWebConsolePlugin.LABEL),
        @Property(name = WebConsoleConstants.PLUGIN_TITLE, value = DiscoveryWebConsolePlugin.TITLE),
        @Property(name = "felix.webconsole.configprinter.modes", value = "zip")
})
public class DiscoveryWebConsolePlugin extends AbstractWebConsolePlugin {

    protected static final String LABEL = "topology";

    protected static final String TITLE = "Topology Viewer";

    @Reference
    private DiscoveryService discoveryService;

    @Override
    protected void renderContent(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        Object rawRoot = req.getAttribute(WebConsoleConstants.ATTR_PLUGIN_ROOT);
        if (!(rawRoot instanceof String)) {
            throw new ServletException("Illegal attr: " + WebConsoleConstants.ATTR_PLUGIN_ROOT);
        }
        String root = rawRoot.toString();
        String pathInfo = req.getRequestURI().substring(root.length());
        // load view if needed
        EtcdDiscoveryService ds = (EtcdDiscoveryService)discoveryService;
        if (ds != null) {
            TopologyView view = ds.getTopology();
            if ("".equals(pathInfo) || "/".equals(pathInfo)) {
                renderOverview(res.getWriter(), view, ds.getContext());
            } else if ("/statistics".equals(pathInfo)) {
                renderStatistics(res.getWriter(), ds);
            } else {
                StringTokenizer tokenizer = new StringTokenizer(pathInfo, "/");
                String slingId = tokenizer.nextToken();
                renderDetails(res.getWriter(), view, slingId);
            }
        } else {
            throw new ServletException("DiscoveryService is not available");
        }
    }

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    private void renderOverview(@Nonnull PrintWriter pw, @Nonnull TopologyView view, @Nonnull Context context) {
        renderMenu(pw);
        final String changing;
        if (!view.isCurrent()) {
            changing = " <b><i>changing</i></b> ";
        } else {
            changing = " ";
        }
        final String state = "in state <b><i>" + context.getState() + "</i></b>";
        pw.println("<p class=\"statline ui-state-highlight\">Topology" + changing + state + "</p>");
        pw.println("<div class=\"ui-widget-header ui-corner-top buttonGroup\" style=\"height: 15px;\">");
        pw.println("<span style=\"float: left; margin-left: 1em;\">Instances in the topology</span>");
        pw.println("</div>");
        pw.println("<table class=\"adapters nicetable ui-widget tablesorter\">");
        pw.println("<thead>");
        pw.println("<tr>");
        pw.println("<th class=\"header ui-widget-header\">Sling id (click for properties)</th>");
        pw.println("<th class=\"header ui-widget-header\">ClusterView id</th>");
        pw.println("<th class=\"header ui-widget-header\">Local instance</th>");
        pw.println("<th class=\"header ui-widget-header\">Leader instance</th>");
        pw.println("<th class=\"header ui-widget-header\">In local cluster</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody>");
        ClusterView localView = view.getLocalInstance().getClusterView();
        for (ClusterView clusterView : view.getClusterViews()) {
            boolean inLocalCluster = localView.getId().equals(clusterView.getId());
            for (InstanceDescription description : clusterView.getInstances()) {
                renderInstance(pw, description, inLocalCluster);
            }
        }
        pw.println("</tbody>");
        pw.println("</table>");
    }

    private void renderStatistics(@Nonnull PrintWriter pw, @Nonnull EtcdDiscoveryService etcdDiscoveryService) {
        renderMenu(pw);
        EtcdStats etcdStats = etcdDiscoveryService.getEtcdStats();
        EtcdStats.Stats stats = (etcdStats != null) ? etcdStats.getStats() : null;
        if (stats != null) {
            List<Member> members = stats.getMembers().members();
            pw.println("<p class=\"statline ui-state-highlight\">etcd statistics (at " + stats.fetchTime().getTime() + ")</p>");
            pw.println("<table class=\"adapters nicetable ui-widget tablesorter\">");
            pw.println("<thead>");
            pw.println("<tr>");
            pw.println("<th class=\"header ui-widget-header\">Peer id</th>");
            for (Member member : members) {
                pw.println("<th class=\"header ui-widget-header\">" + member.id() + "</th>");
            }
            pw.println("</tr>");
            pw.println("</thead>");
            pw.println("<tbody>");

            pw.println("<tr>");
            pw.println("<td>Name</td>");
            for (Member member : members) {
                pw.println("<td>" + member.name() + "</td>");
            }
            pw.println("</tr>");

            pw.println("<tr>");
            pw.println("<td>Client URLs</td>");
            for (Member member : members) {
                pw.println("<td>");
                for (Iterator<URI> urls = member.clientUrls().iterator() ; urls.hasNext() ; ) {
                    pw.print(urls.next());
                    if (urls.hasNext()) {
                        pw.print("<br/>");
                    }
                }
                pw.println("</td>");
            }
            pw.println("</tr>");

            pw.println("<tr>");
            pw.println("<td>Peer URLs</td>");
            for (Member member : members) {
                pw.println("<td>");
                for (Iterator<URI> urls = member.peerUrls().iterator() ; urls.hasNext() ; ) {
                    pw.print(urls.next());
                    if (urls.hasNext()) {
                        pw.print("<br/>");
                    }
                }
                pw.println("</td>");
            }
            pw.println("</tr>");

            // peer stats

            renderMemberStat(pw, members, stats.getMembersStats(), "Leader id", new MemberExtractor() {
                public String get(@Nonnull MemberStatsResponse response) {
                    return String.valueOf(response.leaderId());
                }
            });

            renderMemberStat(pw, members, stats.getMembersStats(), "Leader uptime", new MemberExtractor() {
                public String get(@Nonnull MemberStatsResponse response) {
                    return String.valueOf(response.leaderUptime());
                }
            });

            renderMemberStat(pw, members, stats.getMembersStats(), "Leader start time", new MemberExtractor() {
                public String get(@Nonnull MemberStatsResponse response) {
                    return String.valueOf(response.leaderStartTime().getTime());
                }
            });

            renderMemberStat(pw, members, stats.getMembersStats(), "State", new MemberExtractor() {
                public String get(@Nonnull MemberStatsResponse response) {
                    return String.valueOf(response.state());
                }
            });

            renderMemberStat(pw, members, stats.getMembersStats(), "Nb received append requests", new MemberExtractor() {
                public String get(@Nonnull MemberStatsResponse response) {
                    return String.valueOf(response.recvAppendRequestCnt());
                }
            });

            renderMemberStat(pw, members, stats.getMembersStats(), "Receiving data rate [Byte/s]", new MemberExtractor() {
                public String get(@Nonnull MemberStatsResponse response) {
                    return String.valueOf(response.recvBandwidthRate());
                }
            });

            renderMemberStat(pw, members, stats.getMembersStats(), "Receiving request rate [req/s]", new MemberExtractor() {
                public String get(@Nonnull MemberStatsResponse response) {
                    return String.valueOf(response.recvPkgRate());
                }
            });

            renderMemberStat(pw, members, stats.getMembersStats(), "Nb append requests sent", new MemberExtractor() {
                public String get(@Nonnull MemberStatsResponse response) {
                    return String.valueOf(response.sendAppendRequestCnt());
                }
            });

            renderMemberStat(pw, members, stats.getMembersStats(), "Sending data rate [Byte/s]", new MemberExtractor() {
                public String get(@Nonnull MemberStatsResponse response) {
                    return String.valueOf(response.sendBandwidthRate());
                }
            });


            renderMemberStat(pw, members, stats.getMembersStats(), "Sending request rate [req/s]", new MemberExtractor() {
                public String get(@Nonnull MemberStatsResponse response) {
                    return String.valueOf(response.sendPkgRate());
                }
            });

            renderMemberStat(pw, members, stats.getMembersStats(), "Start Time", new MemberExtractor() {
                public String get(@Nonnull MemberStatsResponse response) {
                    return response.startTime().getTime().toString();
                }
            });

            // leader stats

            LeaderStatsResponse leaderStatsResponse = stats.getLeaderStats();
            if (leaderStatsResponse != null) {
                List<FollowerStats> followers = leaderStatsResponse.followers();

                renderLeaderStat(pw, members, followers, "Successful Raft RPC count", new FollowerExtractor() {
                    public String get(@Nonnull FollowerStats follower) {
                        return String.valueOf(follower.successCount());
                    }
                });

                renderLeaderStat(pw, members, followers, "Failed Raft RPC count", new FollowerExtractor() {
                    public String get(@Nonnull FollowerStats follower) {
                        return String.valueOf(follower.failCount());
                    }
                });

                renderLeaderStat(pw, members, followers, "Latency [ms]", new FollowerExtractor() {
                    public String get(@Nonnull FollowerStats follower) {
                        return String.valueOf(follower.latencyCurrent());
                    }
                });

                renderLeaderStat(pw, members, followers, "Latency Avg [ms]", new FollowerExtractor() {
                    public String get(@Nonnull FollowerStats follower) {
                        return String.valueOf(follower.latencyAverage());
                    }
                });

                renderLeaderStat(pw, members, followers, "Latency Max [ms]", new FollowerExtractor() {
                    public String get(@Nonnull FollowerStats follower) {
                        return String.valueOf(follower.latencyMax());
                    }
                });

                renderLeaderStat(pw, members, followers, "Latency Min [ms]", new FollowerExtractor() {
                    public String get(@Nonnull FollowerStats follower) {
                        return String.valueOf(follower.latencyMin());
                    }
                });

                renderLeaderStat(pw, members, followers, "Latency Std dev.", new FollowerExtractor() {
                    public String get(@Nonnull FollowerStats follower) {
                        return String.valueOf(follower.latencyStdDev());
                    }
                });


            }

            pw.println("</tbody>");
            pw.println("</table>");

        } else {
            pw.println("<p>etcd statistics not yet available</p>");
        }
    }

    private void renderMemberStat(@Nonnull PrintWriter pw, @Nonnull List<Member> members, @Nonnull Map<String, MemberStatsResponse> membersStats, @Nonnull String statName, @Nonnull MemberExtractor extractor) {
        pw.println("<tr>");
        pw.println("<td>" + statName + "</td>");
        for (Member member : members) {
            MemberStatsResponse memberStats = membersStats.get(member.id());
            if (memberStats != null) {
                pw.println("<td>" + extractor.get(memberStats) + "</td>");
            } else {
                pw.println("<td></td>");
            }
        }
        pw.println("</tr>");
    }

    private void renderLeaderStat(@Nonnull PrintWriter pw, @Nonnull List<Member> members, @Nonnull List<FollowerStats> followers, @Nonnull String statName, @Nonnull FollowerExtractor extractor) {
        pw.println("<tr>");
        pw.println("<td>" + statName + "</td>");
        for (Member member : members) {
            FollowerStats follower = find(followers, member.id());
            if (follower != null) {
                pw.println("<td>" + extractor.get(follower) + "</td>");
            } else {
                pw.println("<td></td>");
            }
        }
        pw.println("</tr>");
    }

    private FollowerStats find(@Nonnull List<FollowerStats> followers, @Nonnull String id) {
        for (FollowerStats follower : followers) {
            if (follower.id().equals(id)) {
                return follower;
            }
        }
        return null;
    }

    private void renderMenu(@Nonnull PrintWriter pw) {
        pw.println("<br/>");
        pw.println("<a href=\"${appRoot}/" + getLabel() + "\" class=\"ui-state-default ui-corner-all\">Topology</a> ");
        pw.println("<a href=\"${appRoot}/" + getLabel() + "/statistics\" class=\"ui-state-default ui-corner-all\">Statistics</a> ");
        pw.print("<a href=\"${appRoot}/configMgr/org.apache.sling.discovery.etcd.EtcdDiscoveryService\" class=\"ui-state-default ui-corner-all\">Configure Discovery Service</a>");
        pw.println("<br/>");
        pw.println("<br/>");
    }

    private void renderDetails(@Nonnull PrintWriter pw, @Nonnull TopologyView view, @Nonnull String slingId) {
        renderMenu(pw);
        for (Iterator<InstanceDescription> iterator = view.getInstances().iterator() ; iterator.hasNext() ; ) {
            InstanceDescription instance = iterator.next();
            if (slingId.equals(instance.getSlingId())) {
                renderProperties(pw, instance);
            }
        }
    }

    private void renderInstance(@Nonnull PrintWriter pw, @Nonnull InstanceDescription description, boolean inLocalCluster) {
        String slingId = description.getSlingId();
        boolean isLocal = description.isLocal();
        pw.print("<td>");
        if (isLocal) {
            pw.print("<b>");
        }
        pw.print("<a href=\"");
        pw.print(getLabel());
        pw.print('/');
        pw.print(slingId);
        pw.print("\">");
        pw.print(slingId);
        pw.print("</a>");
        if (isLocal) {
            pw.print("</b>");
        }
        pw.println("</td>");
        pw.println("<td>");
        pw.print(description.getClusterView().getId());
        pw.print("</td>");
        pw.println("<td>" + (isLocal ? "<b>true</b>" : "false") + "</td>");
        pw.println("<td>");
        pw.print(description.isLeader() ? "<b>true</b>" : "false");
        pw.print("</td>");
        pw.println("<td>");
        if (inLocalCluster) {
            pw.print("local");
        } else {
            pw.print("remote");
        }
        pw.print("</td>");
        pw.println("</tr>");
    }

    private void renderProperties(@Nonnull PrintWriter pw, @Nonnull InstanceDescription instance) {
        pw.println("<p class=\"statline ui-state-highlight\">Properties of " + instance.getSlingId() + "</p>");
        pw.println("<table class=\"adapters nicetable ui-widget tablesorter\">");
        pw.println("<thead>");
        pw.println("<tr>");
        pw.println("<th class=\"header ui-widget-header\">Key</th>");
        pw.println("<th class=\"header ui-widget-header\">Value</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody>");
        boolean odd = true;
        for (Iterator<Map.Entry<String, String>> it = instance.getProperties()
                .entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, String> entry = it.next();
            String oddEven = odd ? "odd" : "even";
            odd = !odd;
            pw.println("<tr class=\"" + oddEven + " ui-state-default\">");

            pw.println("<td>" + entry.getKey() + "</td>");
            pw.println("<td>" + entry.getValue() + "</td>");

            pw.println("</tr>");
        }
        pw.println("</tbody>");
        pw.println("</table>");
    }

    private interface MemberExtractor {
        String get(@Nonnull MemberStatsResponse response);
    }

    private interface FollowerExtractor {
        String get(@Nonnull FollowerStats follower);
    }

}
