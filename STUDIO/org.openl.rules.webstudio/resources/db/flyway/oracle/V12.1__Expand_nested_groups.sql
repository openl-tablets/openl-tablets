-- Decompose group hierarch
INSERT INTO OpenL_Group2Group(groupID, includedGroupID)
WITH Hierarchy(groupID, includedGroupID, Depth) AS (
    -- Base case: select all direct parent-child relationships, starting with depth 1
    SELECT groupID, includedGroupID, 1 AS Depth
    FROM OpenL_Group2Group
    UNION ALL
    -- Recursive case: find children of the current child and increase the depth
    SELECT h.groupID, yt.includedGroupID, h.Depth + 1
    FROM Hierarchy h
             JOIN OpenL_Group2Group yt ON h.includedGroupID = yt.groupID
    -- Limit the depth to a specific maximum level
    WHERE h.Depth < 15)
-- Aggregate the results by parent, collecting all unique children
SELECT DISTINCT groupID, includedGroupID
FROM Hierarchy t1
WHERE groupID <> includedGroupID
  AND NOT EXISTS(SELECT groupID
                 FROM OpenL_Group2Group t2
                 WHERE t2.groupID = t1.groupID
                   AND t2.includedGroupID = t1.includedGroupID);
