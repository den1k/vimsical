### Retrieve Mailing List Signups
```
bash-4.4$ for stream
in `aws logs describe-log-streams --log-group-name /aws/lambda/handler | jq -r '.logStreams | map(.logStreamName) | to_entries[] | (.value)'` ; do
    aws logs get-log-events \
        --log-group-name /aws/lambda/handler \
        --log-stream-name $stream --output text | grep email
done
```